(ns trivia-cms.models.question
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [trivia-cms.db.config :refer :all]
            [trivia-cms.errors.api-error :refer [api-error]]
            [trivia-cms.models.orm :as orm :refer [create-id]]
            [trivia-cms.api.public-api :as public-api :refer [IPublicAPI]])
  (:use monger.operators)
  (:import org.bson.types.ObjectId))

(def collection-name "questions")

(defrecord Question [_id body answer category value]
  IPublicAPI

  (serialize [this]
    {:id (.toString _id)
     :question body
     :answer answer
     :category category
     :value value}))

(defmethod orm/adapter trivia_cms.models.question.Question [_ attrs]
  (let [{:keys [_id body answer category value]} attrs] 
    (if (some nil? [_id body answer category value])
      nil
      (->Question _id body answer category value))))

(defn find-models [cond]
  (orm/find Question cond orm/adapter))

(defn create [params]
  (let [{:keys [body answer category value]} params
        validated? (not (some nil? [body answer category value]))]
    (if validated?
      (let [id (create-id (:_id params))
            question (->Question id
                                 body
                                 answer
                                 category
                                 value)]
        (orm/create Question question orm/adapter))
      (api-error "Question failed to create."))))

(defn destroy [^String id]
  (try
    (.getN
     (mc/remove-by-id db-handle collection-name (ObjectId. id)))
    (catch Exception e 
      (do (println "Exception: " (.getMessage e))
          0))))
