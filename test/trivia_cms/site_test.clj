(ns trivia-cms.site-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [ring.mock.request :as mock]
            [trivia-cms.handler :refer :all]
            [trivia-cms.db.config :refer :all]
            [monger.core :as mg]
            [monger.collection :as mc]))

(deftest test-site

  (testing "home page"
    (let [response (app (mock/request  :get "/"))]
      (is (= (:status response) 200))
      (is (re-matches #"(.*)text/html(.*)" (get (:headers response) "Content-Type")))))
  
  (testing "get all quizzes"
    (let [response (app (mock/request :get "/quizzes"))]
      (is (= (:status response) 200))))

  (testing "get a single quiz"
    (let [response (app (mock/request :get "/quizzes/test_quiz_1"))]
      (is (= (:status response) 200)))))
