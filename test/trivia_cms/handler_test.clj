(ns trivia-cms.handler-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [ring.mock.request :as mock]
            [trivia-cms.handler :refer :all]
            [trivia-cms.db.config :refer :all]
            [monger.core :as mg]
            [monger.collection :as mc]))

(def test-quiz-1 
  {:questions [{:question "test question 1"
              :category "test category 1"
              :answer "test answer 1"
              :value 1}]
   :quiz-name "test_quiz_1"})

(def test-quiz-2 
  {:questions [{:question "test question 2"
              :category "test category 2"
              :answer "test answer 2"
              :value 2}]
   :quiz-name "test_quiz_2"})

(def test-quiz-3-to-delete
  {:questions [{:question "test question 3"
                :category "test category 3"
                :answer "test answer 3"
                :value 3}]
   :quiz-name "test_quiz_3_to_delete"})

(def test-quiz-4-to-create
  {:questions [{:question "test question 4"
                :category "test category 4"
                :answer "test answer 4"
                :value 4}]
   :quiz-name "test_quiz_4_to_create"})

(def test-question-1
  {:question-body "test question 1 update"
   :category "test question 1 category update"
   :answer "test question 1 answer update"
   :value 2})

(defn init-db []
  (println "Seeding test database...")
  (mc/insert db-handle quizzes-collection-name test-quiz-1)
  (mc/insert db-handle quizzes-collection-name test-quiz-2))

(defn teardown-db []
  (println "Removing all records from test database...")
  (mc/remove db-handle quizzes-collection-name))

(defn trivia-fixture [f]
  (init-db)
  (f)
  (teardown-db))

(use-fixtures :once trivia-fixture)

(deftest test-api

  (testing "create quiz api - invalid parameters"
    (let [response (app 
                    (-> 
                     (mock/request :post "/api/quizzes/create" 
                                   (json/write-str {}))
                     (mock/content-type "application/json")))]
      (is (= (:status response) 400))))

  (testing "create quiz api - valid, no questions"
    (let [response (app 
                    (->
                     (mock/request :post 
                                   "/api/quizzes/create" 
                                   (json/write-str {:quiz-name "first quiz"}))
                     (mock/content-type "application/json")))]
      (is (= (:status response) 200))))

  (testing "create quiz api - valid parameters"
    (let [response (app 
                    (->
                      (mock/request :post "/api/quizzes/create" (json/write-str test-quiz-4-to-create))
                      (mock/content-type "application/json")))]
      (is (= (:status response) 200))))

  (testing "get quiz api - invalid quiz"
    (let [response (app (mock/request :get "/api/quizzes/invalid"))]
      (is (= (:status response) 404))
      (is (= (:body response) (json/write-str {:error-message "Quiz 'invalid' not found."})))))

  (testing "get quiz api - valid quiz"
    (let [response (app 
                    (mock/request :get (str "/api/quizzes/" (:quiz-name test-quiz-2))))]
      (is (= (:status response) 200))
      (is (= (dissoc (json/read-str (:body response) :key-fn keyword) :_id) 
             test-quiz-2))))

  (testing "get quizzes api"
    (let [response (app
                    (mock/request :get "/api/quizzes"))]
      (is (= (:status response) 200))
      (is (= 4 (count (json/read-str (:body response)))))))

  (testing "delete quiz api - delete a quiz"
    (do
      (mc/insert db-handle quizzes-collection-name test-quiz-3-to-delete)
      (let [response (app 
                      (mock/request 
                       :delete 
                       (str "/api/quizzes/" (:quiz-name test-quiz-3-to-delete))))]
        (is (= (:status response) 200))
        (is (= (json/read-str (:body response) :key-fn keyword) {:num-deleted 1})))))

  (testing "delete quiz api - delete an invalid quiz"
    (let [response (app (mock/request :delete "/api/quizzes/invalid"))]
      (is (= (:status response) 404))
      (is (= (json/read-str (:body response) :key-fn keyword) 
             {:error-message "Quiz 'invalid' not found."}))))

  (testing "create question api - create a new question"
    (let [path (str "/api/quizzes/" (:quiz-name test-quiz-1) "/questions/create")
          response (app 
                    (->
                     (mock/request 
                      :post 
                      path
                      (json/write-str 
                       (assoc test-question-1 
                         :quiz-name (:quiz-name test-quiz-1))))
                     (mock/content-type "application/json")))]
      (is (= (:status response) 200))
      (is (= (dissoc (json/read-str (:body response) :key-fn keyword) :_id) 
             (update-in
              test-quiz-1
              [:questions]
              #(concat % [test-question-1]))))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]

      (is (= (:status response) 404)))))
