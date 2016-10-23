(ns trivia-cms.handler-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [ring.mock.request :as mock]
            [trivia-cms.handler :refer :all]
            [trivia-cms.db.config :refer :all]
            [trivia-cms.models.quiz :refer [->Quiz] :as quiz]
            [monger.core :as mg]
            [monger.collection :as mc])
  (:use [clojure.walk])
  (:import org.bson.types.ObjectId))

(def test-question-1 
  {:question-body "test question 1"
   :category "test category 1"
   :answer "test answer 1"
   :_id (ObjectId.)
   :value 1})

(def test-question-2
  {:question-body "test question 2"
   :category "test category 2"
   :answer "test answer 2"
   :_id (ObjectId.)
   :value 2})

(def test-question-3
  {:question-body "test question 3"
   :category "test category 3"
   :answer "test answer 3"
   :_id (ObjectId.)
   :value 3})

(def test-question-4
  {:body "test question 4"
   :category "test category 4"
   :answer "test answer 4"
   :_id (.toString (ObjectId.))
   :value 4})

(def test-quiz-1 
  (->Quiz
   (ObjectId.)
   "test_quiz_1"
   [(:_id test-question-1)]))

(def test-quiz-2 
  (->Quiz
   (ObjectId.)
   "test_quiz_2"
   [(:_id test-question-2)]))

(def test-quiz-3-to-delete
  {:questions [(:_id test-question-3)]
   :quiz-name "test_quiz_3_to_delete"
   :_id (ObjectId.)})

(def test-quiz-4-to-create
  {:quiz-name "test_quiz_4_to_create"
   :questions [test-question-4]
   :_id (.toString (ObjectId.))})

(def test-question-1-create
  {:question-body "test question 1 create"
   :category "test question 1 category create"
   :answer "test question 1 answer create"
   :value 2
   :_id (ObjectId.)})

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

(use-fixtures :each trivia-fixture)

(deftest test-api
  (testing "get quizzes api"
    (let [response (app (mock/request :get "/api/quizzes"))]
      (is (= (count 
              (json/read-str 
               (:body response)))
             2))
      (is (= (keywordize-keys (json/read-str (:body response)))
             (map quiz/serialize [test-quiz-1 test-quiz-2])))))
  
  (testing "create quizzes api"
    (let [response (app (->
                         (mock/request :post "/api/quizzes/create"
                                       (json/write-str test-quiz-4-to-create))
                         (mock/content-type "application/json")))
          parsed-response-body (keywordize-keys (json/read-str (:body response)))]
      (is (= (:status response) 200))
      (is (= (:name parsed-response-body)
             (:quiz-name test-quiz-4-to-create)))
      (is (= (count (:question-ids parsed-response-body))
             (count (:questions test-quiz-4-to-create))))
      (is (= (first (:question-ids parsed-response-body))
             (:_id (first (:questions test-quiz-4-to-create)))))))

  (testing "delete quizzes api"
    (let [response (app 
                    (mock/request 
                     :delete 
                     (str "/api/quizzes/" (.toString (:_id test-quiz-1)))))]
      (is (= (:status response) 200))
      (is (= (:num-deleted
              (keywordize-keys 
               (json/read-str 
                (:body response))))
             1)))))

(comment(deftest test-api

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
                      (mock/request 
                       :post 
                       "/api/quizzes/create" 
                       (json/write-str test-quiz-4-to-create))
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
                        test-question-1-create))
                      (mock/content-type "application/json")))]
       (is (= (:status response) 200))
       (let [questions (:questions (keywordize-keys (json/read-str (:body response))))]
         (is (= (count questions) 2))
         (is (= (map #(dissoc % :_id) questions)
                (map #(dissoc % :_id) 
                     (conj (:questions test-quiz-1) test-question-1-create)))))))

   (testing "delete question api - delete a question"
     (let [response (app (mock/request :delete "/api/quizzes/test_quiz_1/questions/1"))]
       (is (= (:status response) 200))
       (is (= (:body response) 1))))

   (testing "not-found route"
     (let [response (app (mock/request :get "/invalid"))]
       (is (= (:status response) 404))))))
