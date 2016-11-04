(ns trivia-cms.models.question
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [trivia-cms.db.config :refer :all]
            [trivia-cms.errors.api-error :refer [api-error]]
            [trivia-cms.models.orm :as orm]
            [trivia-cms.models.public-api :as public-api :refer [IPublicAPI]])
  (:use monger.operators)
  (:import org.bson.types.ObjectId))

(def collection-name "questions")

(defrecord Question [_id body answer category value]
  IPublicAPI

  (serialize [this]
    {:id (.toString _id)
     :body body
     :answer answer
     :category category
     :value value}))

(defmethod orm/adapter trivia_cms.models.question.Question [_ attrs]
  (let [{:keys [_id body answer category value]} attrs] 
    (if (some nil? [_id body answer category value])
      nil
      (->Question _id body answer category value))))

(defn question-adapter [{:keys [_id body answer category value]}]
  (if (some nil? [_id body answer category value])
    nil
    (->Question _id body answer category value)))

(defn find-models [cond]
  (orm/find Question cond orm/adapter))

(defn create [params]
  (let [{:keys [body answer category value]} params
        validated? (not (some nil? [body answer category value]))]
    (if validated?
      (let [id (if (nil? (:_id params))
                  (ObjectId.) 
                  (ObjectId. (:_id params)))
            question (->Question id
                                 body
                                 answer
                                 category
                                 value)]
        (mc/insert-and-return db-handle collection-name question)
        question)
      (api-error "Question failed to create."))))

(defn destroy [^String id]
  (.getN
   (mc/remove-by-id db-handle collection-name (ObjectId. id))))
