(ns trivia-cms.user-login-test
  (:require [clojure.test :refer :all]
            [trivia-cms.models.user :as user]
            [trivia-cms.api.user-login :refer :all]
            [trivia-cms.db.config :refer :all]
            [monger.core :as mg]
            [monger.collection :as mc]
            [buddy.hashers :as hashers]))

(def test-user-token (atom ""))

(def mock-user
  (atom {:username "test" :password-hash (hashers/encrypt "password")}))

; Seed database
(defn init-db []
  (println "Seeding test database...")
  (mc/insert-and-return db-handle user/collection-name @mock-user))

(defn teardown-db []
  (println "Removing all records from test database...")
  (comment(mc/remove db-handle user/collection-name)))

(defn user-login-fixture [f]
  (init-db)
  (f)
  (teardown-db))

(use-fixtures :each user-login-fixture)

(deftest test-user-login
  (testing "login"
    (let [res (login "test" "password" {})
          headers (:headers res)]
      (is (not (nil? (get headers "Set-Cookie"))))
      (swap! 
       mock-user 
       update-in 
       [:token] 
       (fn [_] 
         (second
          (clojure.string/split (get headers "Set-Cookie") #"="))))))

  (testing "is authenticated"
    (let [mock-request {:cookies {"token" {:value (:token @mock-user)} "username" {:value "test"}}}
          res (is-authenticated mock-request)]
      (is (= true res))))
  
  (testing "logout"
    (let [res (logout "test")]
      (is (= "1" (:body res))))))
