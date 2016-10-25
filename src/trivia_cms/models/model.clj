(ns trivia-cms.models.model
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [trivia-cms.db.config :refer :all])
  (:import org.bson.types.ObjectId)
  (:use monger.operators))

(defn update-models [collection-name cond key val]
  (try
    (mc/find-and-modify
     db-handle
     collection-name
     cond
     {$set {key val}}
     {:return-new true})
    (catch Exception e (println (str "Exception: " (.getMessage e))))))

(defn -find-model-by-id 
  "Finds models based on id."
  [^String collection-name ^String id]
  (try
    (conj [] (mc/find-one-as-map db-handle collection-name {:_id (ObjectId. id)}))
    (catch Exception e (println (str "Exception: " (.getMessage e))))))

(defn find-models
  "Finds models based on given hash of conditions.
  If :_id is present in `cond`, `find-models-by-id` is used."
  [^String collection-name cond adapter]
  (if (not (nil? (:_id cond)))
    (let [q (-find-model-by-id collection-name (.toString (:_id cond)))]
      (when (not (nil? q)) 
        (map adapter q)))
    (map adapter
         (mc/find-maps db-handle collection-name cond))))
