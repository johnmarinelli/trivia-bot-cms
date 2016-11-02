(ns trivia-cms.model-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [trivia-cms.db.config :refer :all]
            [trivia-cms.models.quiz :refer [->Quiz] :as quiz]
            [trivia-cms.models.question :refer [->Question] :as question]
            [monger.core :as mg]
            [monger.collection :as mc])
  (:import [org.bson.types ObjectId]))

(def test-question-1
  (->Question (ObjectId.) 
              "test question "
              "test question category"
              1
              "test question answer"))

(def test-quiz-1
  (->Quiz (ObjectId.) 
          "test-quiz-1"
          [(.toString (:_id test-question-1))]))

(defn init-db []
  (println "Seeding test database...")
  (mc/insert db-handle question/collection-name test-question-1)
  (mc/insert db-handle quiz/collection-name test-quiz-1))

(defn teardown-db []
  (println "Destroying test database...")
  (mc/remove db-handle question/collection-name {})
  (mc/remove db-handle quiz/collection-name {}))

(defn model-fixture [f]
  (init-db)
  (f)
  (teardown-db))

(use-fixtures :each model-fixture)

(deftest test-models
  (deftest test-quizzes

    (testing "quiz models - find by id"
      (let [id (.toString (:_id test-quiz-1))
            res (quiz/find-models {:_id id})]
        (is (= (count res) 1))
        (is (= (first res) test-quiz-1))))


    (testing "quiz models - find by name"
      (let [name (:quiz-name test-quiz-1)
            res (quiz/find-models {:quiz-name name})]
        (is (= (count res) 1))
        (is (= (first res) test-quiz-1))))

    (testing "quiz models - find by INVALID name"
      (let [name "invalid"
            res (quiz/find-models {:quiz-name name})]
        (is (= (count res) 0))
        (is (= (first res) nil))))

    (testing "quiz models - add questions"
      (let [question (->Question (ObjectId.)
                                 "a"
                                 "b"
                                 "c"
                                 "d")
            res (quiz/add-questions test-quiz-1 [question])]
        (is (= (count (:questions res)) 2))
        (is (= (:questions res) 
               (conj (:questions test-quiz-1) (.toString (:_id question)))))))

    (testing "quiz models - remove questions"
      (let [num-questions (count 
                           (:questions
                            (first
                             (quiz/find-models 
                              {:_id (.toString (:_id test-quiz-1))}))))
            res (quiz/remove-questions test-quiz-1 [(:_id test-question-1)])]
        (is (= (count (:questions res)) (dec num-questions)))))

    (testing "quiz models - create with no questions"
     (let [quiz-name "test-quiz"
           params {:quiz-name quiz-name :questions '()}
           created (quiz/create params)]
       (is (= (count (quiz/find-models {})) 2))
       (is (= (count (:questions created)) 0))
       (is (= (:quiz-name created) "test-quiz"))))
    
    (testing "quiz models - destroy"
      (let [id (.toString (:_id test-quiz-1))
            res (quiz/destroy id)]
        (is (= res 1)))))

  (deftest test-questions
    (testing "question models - create"
      (let [params {:body "body"
                    :answer "answer"
                    :value 1
                    :category "category"}
            created (question/create params)]
        (is (= (dissoc created :_id) params))))

    (testing "question models - find by id"
      (let [id (.toString (:_id  test-question-1))
            res (question/find-models {:_id id})]
        (is (= (count res) 1))
        (is (= (first res) test-question-1))))

    (testing "question models - destroy"
      (let [id (.toString (:_id test-question-1))
            res (question/destroy id)]
        (is (= res 1))))))
