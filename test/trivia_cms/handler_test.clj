(ns trivia-cms.handler-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [ring.mock.request :as mock]
            [trivia-cms.handler :refer :all]
            [trivia-cms.db.config :refer :all]
            [trivia-cms.models.quiz :refer [->Quiz] :as quiz]
            [trivia-cms.models.question :refer [->Question] :as question]
            [trivia-cms.api.public-api :as public-api]
            [monger.core :as mg]
            [monger.collection :as mc])
  (:use [clojure.walk])
  (:import org.bson.types.ObjectId))

(def test-question-1
  (->Question
   (ObjectId.)
   "test question 1"
   "test category 1"
   1
   "test answer 1"))

(def test-question-2
  (->Question
   (ObjectId.)
   "test question 2"
   "test category 2"
   2
   "test answer 2"))

(def test-question-3
  (->Question
   (ObjectId.)
   "test question 3"   
   "test category 3"
   3
   "test answer 3"))

(def test-question-4-to-create
  {:body "test question 4"
   :category "test category 4"
   :value 4
   :answer "test answer 4"
   :_id (.toString (ObjectId.))})

(def test-quiz-1 
  (->Quiz
   (ObjectId.)
   "test_quiz_1"
   [(.toString (:_id test-question-1))]))

(def test-quiz-2 
  (->Quiz
   (ObjectId.)
   "test_quiz_2"
   [(.toString (:_id test-question-2))]))

(def test-quiz-3-to-delete
  {:questions [(.toString (:_id test-question-3))]
   :quiz-name "test_quiz_3_to_delete"
   :_id (.toString (ObjectId.))})

(def test-quiz-4-to-create
  {:quiz-name "test_quiz_4_to_create"
   :questions [test-question-4-to-create]
   :_id (.toString (ObjectId.))})

(def test-question-2-create-json
  {:body "test question 2 create"
   :category "test question 2 category create"
   :answer "test question 2 answer create"
   :value 2
   :_id (.toString (ObjectId.))})

(defn init-db []
  (println "Seeding test database...")
  (mc/insert db-handle quizzes-collection-name test-quiz-1)
  (mc/insert db-handle quizzes-collection-name test-quiz-2)
  (mc/insert db-handle questions-collection-name test-question-1)
  (mc/insert db-handle questions-collection-name test-question-2))

(defn teardown-db []
  (println "Removing all records from test database...")
  (mc/remove db-handle quizzes-collection-name))

(defn trivia-fixture [f]
  (init-db)
  (f)
  (teardown-db))

(use-fixtures :each trivia-fixture)

(deftest test-api
  (testing "get all quizzes api"
    (let [response (app (mock/request :get "/api/quizzes"))]
      (is (= (count 
              (json/read-str 
               (:body response)))
             2))
      (is (= (keywordize-keys (json/read-str (:body response)))
             (map public-api/serialize [test-quiz-1 test-quiz-2])))))

  (testing "get a quiz api"
    (let [name (:quiz-name test-quiz-1)
          response (app (mock/request :get (str "/api/quizzes/" name)))
          parsed-response-body (keywordize-keys (json/read-str (:body response)))]
      (is (= (:status response) 200))
      (is (= (:id parsed-response-body) (.toString (:_id test-quiz-1))))
      (is (= (:name parsed-response-body) (:quiz-name test-quiz-1)))
      (is (= (:question-ids parsed-response-body)
             [(public-api/serialize test-question-1)]))))

  (testing "get a quiz by id api"
    (let [id (.toString (:_id test-quiz-1))
          response (app (mock/request :get (str "/api/quizzes/" id)))
          parsed-response-body (keywordize-keys (json/read-str (:body response)))]
      (is (= (:status response) 200))
      (is (= (:id parsed-response-body) (.toString (:_id test-quiz-1))))
      (is (= (:name parsed-response-body) (:quiz-name test-quiz-1) ))
      (is (= (:question-ids parsed-response-body)
             [(public-api/serialize test-question-1)]))))

  (testing "get a quiz by id - invalid id"
    (let [id "80d8f16cd7e947491eed7f2"
          response (app (mock/request :get (str "/api/quizzes/" id)))
          parsed-response-body (keywordize-keys (json/read-str (:body response)))]
      (is (= (:status response) 200))
      (is (not (nil? (re-find (re-pattern id)
                              (:error-message (keywordize-keys (json/read-str (:body response))))))))))
  (testing "create quizzes api"
    (let [response (app (->
                         (mock/request 
                          :post 
                          "/api/quizzes/create"
                          (json/write-str test-quiz-4-to-create))
                         (mock/content-type "application/json")))
          parsed-response-body (keywordize-keys (json/read-str (:body response)))]
      (is (= (:status response) 200))
      (is (= (:name parsed-response-body)
             (:quiz-name test-quiz-4-to-create)))
      (is (= (count (:question-ids parsed-response-body))
             (count (:questions test-quiz-4-to-create))))))

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
             1))))

  (testing "add question to quiz api"
    (let [url (str "/api/quizzes/" (:quiz-name test-quiz-2) "/questions")
          payload (json/write-str test-question-2-create-json)
          response (app (-> (mock/request :post url payload)
                            (mock/content-type "application/json")))
          parsed-response-body (keywordize-keys (json/read-str (:body response)))]
      (is (= (:status response) 200))
      (is (= (:name parsed-response-body)
             (:quiz-name test-quiz-2)))
      (is (= (count (:question-ids parsed-response-body))
             (inc (count (:questions test-quiz-2)))))))

  (testing "remove question from quiz api"
    (let [url (str "/api/quizzes/" (:_id test-quiz-2) "/questions/" (:_id test-question-2))
          num-questions (count (:questions test-quiz-2))
          response (app (mock/request :delete url))]
      (is (= (:status response) 200))
      (is (= (dec num-questions) 
             (count (:question-ids response))))))

  (testing "remove invalid question from quiz api does NOT change question count"
    (let [num-questions (count (:questions test-quiz-2))
          url (str "/api/quizzes/" (:_id test-quiz-2) "/questions/" 1)
          response (app (mock/request :delete url))
          parsed-response-body (keywordize-keys (json/read-str (:body response)))]
      (is (= (:status response) 200))
      (is (= (count (:question-ids parsed-response-body)) num-questions)))))

