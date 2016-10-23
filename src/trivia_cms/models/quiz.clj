(ns trivia-cms.models.quiz
  (:require [trivia-cms.errors.api-error :refer [api-error]]
            [trivia-cms.models.question :as question]
            [monger.core :as mg]
            [monger.collection :as mc]
            [trivia-cms.db.config :refer :all])
  (:use monger.operators)
  (:import org.bson.types.ObjectId))

(def collection-name "quizzes")

(defprotocol PublicAPI
  (serialize [this]))

(defrecord Quiz [_id quiz-name questions]
  PublicAPI

  (serialize [this]
    (let [id (.toString _id)]
      {:id id
       :name quiz-name
       :question-ids (map #(.toString %) questions)})))

(defn find-models
  [cond]
  (map #(->Quiz (:_id %) (:quiz-name %) (:questions %))
       (mc/find-maps db-handle collection-name cond)))

(defn add-questions 
  "Quiz [Question] => Quiz'"
  [^Quiz quiz questions]
  (mc/find-and-modify 
   db-handle 
   collection-name
   {:_id (:_id quiz)}
   {$pushAll {:questions (map :_id questions)}}
   {:return-new true}))

(defn remove-questions 
  "Quiz [Question] => Quiz'"
  [^Quiz quiz questions]
  (let [ids (map :_id questions)
        quiz-id (:_id quiz)]
    (mc/find-and-modify
     db-handle
     collection-name
     {:_id quiz-id}
     {$pullAll { :questions ids}}
     {:return-new true})))

(defn create [params]
  (let [questions (or (:questions params) '())
        quiz-name (:quiz-name params)
        validated (not (or (nil? quiz-name) (empty? quiz-name)))]
    (if validated
      (let [quiz (->Quiz (or 
                          (:_id params) 
                          (ObjectId.)) 
                         (:quiz-name params)
                         (map :_id questions))]
        (mc/insert-and-return db-handle collection-name (dissoc quiz :questions))
        (if (not (empty? questions))
          (for [q questions]
                          (question/create q))
          (add-questions quiz questions))
        quiz)
      (api-error "Quiz failed to create."))))

(defn destroy [^String id]
  (.getN
   (mc/remove-by-id db-handle collection-name (ObjectId. id))))

