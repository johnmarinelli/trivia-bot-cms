(ns trivia-cms.models.quiz
  (:require [trivia-cms.errors.api-error :refer [api-error]]
            [trivia-cms.models.orm :as orm :refer [find adapter]]
            [trivia-cms.models.question :as question]
            [trivia-cms.db.config :refer :all]
            [trivia-cms.models.public-api :as public-api :refer [IPublicAPI]]
            [monger.core :as mg]
            [monger.collection :as mc])
  (:use monger.operators)
  (:import [org.bson.types ObjectId]
           [trivia_cms.models.question Question]))

(def collection-name "quizzes")

(defn get-quiz-questions [question-ids]
  (flatten
   (map
    (fn [qid]
      (question/find-models {:_id qid}))
    question-ids)))

(defrecord Quiz [_id quiz-name questions]
  IPublicAPI
  (serialize [this]
    (let [id (.toString _id)
          question-models (get-quiz-questions questions)]
      {:id id
       :name quiz-name
       :question-ids (map 
                      (fn [q] (public-api/serialize q)) 
                      (filter identity question-models))})))

(defmethod orm/adapter trivia_cms.models.quiz.Quiz [_ attrs]
  (let [{:keys [_id quiz-name questions]} attrs]
    (when (not (nil? quiz-name)) 
      (->Quiz _id quiz-name questions))))

(defn quiz-adapter [cls params] 
  (let [{:keys [_id quiz-name questions]} params] 
    (->Quiz _id quiz-name questions)))

(defn find-models [cond]
  (orm/find Quiz cond orm/adapter))

; adds question ids to quiz 'questions'.  need to rename
(defn add-questions 
  "Quiz [Question] => Quiz'"
  [^Quiz quiz questions]
  (let [res (mc/find-and-modify 
             db-handle 
             collection-name
             {:_id (:_id quiz)}
             {$pushAll {:questions (map #(.toString (:_id %)) questions)}}
             {:return-new true})]
    (orm/adapter Quiz res)))

(defn remove-questions 
  "Quiz [Question] => Quiz'"
  [^Quiz quiz questions]
  (let [ids (map #(.toString %) questions)
        quiz-id (:_id quiz)]
    (let [r  (mc/find-and-modify
             db-handle
             collection-name
             {:_id quiz-id}
             {$pullAll { :questions ids}}
             {:return-new true})]
      (orm/adapter Quiz r))))

(defn -create-questions-for-quiz [questions]
  (let [res (map question/create (flatten (conj [] questions)))] 
    res))

(defn create [params]
  (let [questions (or (:questions params) [])
        quiz-name (:quiz-name params)
        validated (not (or (nil? quiz-name) (empty? quiz-name)))
        quiz-id (if (not (nil? (:_id params)))
                          (ObjectId. (:_id params))
                          (ObjectId.))]
    (if validated
      (let [quiz (->Quiz quiz-id
                         (:quiz-name params)
                         [])]
        (if (not (empty? questions))
          (do
            (let [qids (map 
                        (fn [q] (.toString (:_id q))) 
                        (-create-questions-for-quiz questions))
                  new-quiz (update-in 
                            quiz 
                            [:questions]
                            (fn [qs] 
                              (concat qs qids)))]
              (let [res (mc/insert-and-return db-handle collection-name new-quiz)]
                new-quiz)))
          (do
            (mc/insert-and-return db-handle collection-name quiz)
            quiz)))
      (api-error "Quiz failed to create."))))

(defn destroy [^String id]
  (.getN
   (mc/remove-by-id db-handle collection-name (ObjectId. id))))

