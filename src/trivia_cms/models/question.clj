(ns trivia-cms.models.question
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [trivia-cms.db.config :refer :all]
            [trivia-cms.errors.api-error :refer [api-error]])
  (:use monger.operators)
  (:import org.bson.types.ObjectId))

(def collection-name "questions")

(defrecord Question [_id body answer category value])

(defn find-models [cond]
  (map #(let [{:keys [_id body answer category value]} %]
          (->Question _id body answer category value)) 
       (mc/find-maps db-handle collection-name cond)))

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
                                 value)
            unique? (not (id-exists? question))]
        (mc/insert-and-return db-handle collection-name question)
        question)
      (api-error "Question failed to create."))))

(defn destroy [^String id]
  (.getN
   (mc/remove-by-id db-handle collection-name (ObjectId. id))))
