(ns trivia-cms.api.api-backend
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [trivia-cms.db.config :refer :all])
  (:use [monger.operators] ; mongodb operators $push, $pull, etc
        ))

(defn save-question! [quiz-name question]
  (mc/find-and-modify db-handle
                      quizzes-collection-name
                      {:quiz-name quiz-name} 
                      {$push {:questions question}}
                      {:return-new true}))

; this calls save-question behind the scenes
(defn save-quiz! [quiz]
  (let [questions (:questions quiz)
        quiz-name (:quiz-name quiz)]
    (let [saved-quiz (first (mc/insert-and-return db-handle quizzes-collection-name (dissoc quiz :questions)))]
      (doseq [q questions]
        (save-question! quiz-name q))
      quiz)))

(defn get-quiz [name]
  (let [qs (mc/find-maps db-handle quizzes-collection-name {:quiz-name name})]
    (if (> (count qs) 0) 
      (update-in 
       (first qs)
       [:_id]
       (fn [qid]
         (.toString qid)))
      nil)))

(defn get-quizzes []
  (mc/find-maps db-handle quizzes-collection-name {}))

(defn delete-question [quiz-name question-id]
  (mc/find-and-modify db-handle
                      quizzes-collection-name
                      (:quiz-name quiz-name)
                      ($pull {:_id question-id})
                      {:return-new true}))

(defn delete-quiz [name]
  (let [res (mc/remove db-handle quizzes-collection-name {:quiz-name name})] 
    (.getN res)))
