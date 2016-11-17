(ns trivia-cms.models.quiz
  (:require [trivia-cms.errors.api-error :refer [api-error]]
            [trivia-cms.models.orm :as orm :refer [find adapter create-id]]
            [trivia-cms.models.question :as question]
            [trivia-cms.db.config :refer :all]
            [trivia-cms.api.public-api :as public-api :refer [IPublicAPI]]
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

(defn find-models [cond]
  (orm/find Quiz cond orm/adapter))

; adds question ids to quiz 'questions'.  need to rename
(defn add-questions 
  "Quiz [Question] => Quiz'"
  [^Quiz quiz questions]
  (let [ids (map #(.toString (:_id %)) questions)
        quiz-id (.toString (:_id quiz))
        res (orm/update Quiz {:_id quiz-id} {$pushAll {:questions ids}} orm/adapter)]
    res))

(defn remove-questions
  [^Quiz quiz question-ids]
  (let [ids (map #(.toString %) question-ids)
        quiz-id (.toString (:_id quiz))
        res (orm/update Quiz {:_id quiz-id} {$pullAll {:questions ids}} orm/adapter)]
    res))

(defn -create-questions-for-quiz [questions]
  (let [res (map question/create (flatten (conj [] questions)))] 
    res))

(defn create [params]
  (let [questions (or (:questions params) [])
        quiz-name (:quiz-name params)
        validated (not (or (nil? quiz-name) (empty? quiz-name)))
        quiz-id (create-id (:_id params))]
    (if validated
      (let [qids (map (fn [q]
                        (when (not (nil? q))
                          (.toString (:_id q))))
                      (-create-questions-for-quiz questions))
            quiz (->Quiz quiz-id 
                         (:quiz-name params) 
                         qids)]
        (orm/create Quiz quiz orm/adapter))
      (api-error "Quiz failed to create."))))

(defn destroy [^String id]
  (try
    (.getN
     (mc/remove-by-id db-handle collection-name (ObjectId. id)))
    (catch Exception e (do 
                         (println "Exception: " (.getMessage e))
                         0))))

