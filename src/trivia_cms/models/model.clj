(ns trivia-cms.models.model
  (:require [monger.core :as mg]
            [monger.collection :as mc])
  (:use monger.operators))

(defn update-models [collection-name cond key val]
  (try
    (mc/find-and-modify
     db-handle
     collection-name
     cond
     {$set {key val}}
     {:return-new true})
    (catch Exception e 
      (println (str "Exception: " (.getMessage e))))))



(defn find-modelss
  "Finds models based on given hash of conditions.
  If :_id is present in `cond`, `find-models-by-id` is used."
  [^String collection-name cond adapter]
  (if (not (nil? (:_id cond)))
    (let [q (-find-model-by-id collection-name (.toString (:_id cond)))]
      (when (not (nil? q)) 
        (map adapter q)))
    (map adapter
         (mc/find-maps db-handle collection-name cond))))

(defprotocol IModel 
  "Interface for database-backed models"
  (table-name-2 [_])
  (find-models-2 [this cond]))

(defrecord Question2 [_id a b c d])
(defrecord Quiz2 [_id a b])
(declare find-models-2-2)

(extend-protocol IModel
  Question2
  (table-name-2 [_] "questions")
  (find-models-2 [this cond] 
    (find-models-2-2 this cond))
  
  Quiz2
  (table-name-2 [_] "quizzes")
  (find-models-2 [this cond]
    (find-models-2-2 this cond)))

(defmulti adapter (fn [t _](class t)))
(defmethod adapter trivia_cms.models.model.Question2 [_ params]
  (let [{:keys [_id body answer category value]} params] 
    (if (some nil? [_id body answer category value])
      nil
      (->Question2 _id body answer category value))))

(defn find-models-2-2 [this cond]
  (if (not (nil? (:_id cond)))
    (let [id (.toString (:_id cond))
          q (-find-model-by-id (table-name-2 this) id)]
      (map (partial adapter this) q))
    (let [res (mc/find-maps db-handle (table-name-2 this) cond)] 
      (map (partial adapter this) res))))


