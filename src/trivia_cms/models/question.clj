(ns trivia-cms.models.question
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [trivia-cms.db.config :refer :all]
            [trivia-cms.models.model :as model]
            [trivia-cms.errors.api-error :refer [api-error]]
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

(defn adapter [{:keys [_id body answer category value]}]
  (if (some nil? [_id body answer category value])
    nil
    (->Question _id body answer category value)))

(defn find-models [cond]
  (let [m (model/find-models
           collection-name
           cond
           adapter)]    
    m))

(defn id-exists? 
  "ObjectId => Boolean"
  [id]
  (> (count (find-models {:_id id})) 0))

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
