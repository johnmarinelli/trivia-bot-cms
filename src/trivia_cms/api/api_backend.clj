(ns trivia-cms.api.api-backend
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [trivia-cms.db.config :refer :all])
  (:use [monger.operators] ; mongodb operators $push, $pull, etc
        ))

(defn parse-id-from-document [d]
  (update-in
   d
   [:_id]
   #(.toString %)))

(defn save-question [quiz-name question]
  (let [res (mc/find-and-modify db-handle
                                quizzes-collection-name
                                {:quiz-name quiz-name} 
                                {$push {:questions 
                                        (assoc question :id (System/currentTimeMillis))}}
                                {:return-new true})] 
    (parse-id-from-document res)))

; this calls save-question behind the scenes
(defn save-quiz [quiz]
  (let [questions (:questions quiz)
        quiz-name (:quiz-name quiz)]
    (let [saved-quiz (first (mc/insert-and-return db-handle quizzes-collection-name (dissoc quiz :questions)))]
      (doseq [q questions]
        (save-question quiz-name q))
      quiz)))

(defn get-quiz [name]
  (let [qs (mc/find-maps db-handle quizzes-collection-name {:quiz-name name})]
    (if (> (count qs) 0) 
      (parse-id-from-document (first qs))
      nil)))

(defn get-quizzes []
  (let [quizzes (mc/find-maps db-handle quizzes-collection-name {})]
    (map parse-id-from-document quizzes)))

(defn delete-question [quiz-name question-id]
  (mc/find-and-modify db-handle
                      quizzes-collection-name
                      {:quiz-name quiz-name}
                      {$pull {"questions"  {:id (read-string question-id)}}}
                      {:return-new true}))

(defn delete-quiz [name]
  (let [res (mc/remove db-handle quizzes-collection-name {:quiz-name name})] 
    (.getN res)))
