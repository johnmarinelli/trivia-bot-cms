(ns trivia-cms.models.orm
  (:require [inflections.core :as inflections]
            [trivia-cms.db.config :refer :all]
            [monger.core :as mg]
            [monger.collection :as mc])
  (:import [org.bson.types ObjectId]))

; if id is nil, create a new id.
; otherwise, return an ObjectId with value of id.
(defn create-id [id]
  (if (nil? id)
    (ObjectId.)
    (ObjectId. (.toString id))))

(defn slugify-class [c]
  (let [full-class-name (.toString c)
        qualified-class-name (second
                              (clojure.string/split full-class-name #"\s"))
        class-name (last 
                    (clojure.string/split qualified-class-name #"\."))]
    (inflections/plural
     (inflections/hyphenate class-name))))

(defmulti adapter (fn [cls & attrs] cls))

(defn -find-model-by-id 
  "Finds models based on id."
  [^String collection-name ^String id]
  (try
     (conj [] 
          (mc/find-one-as-map 
           db-handle 
           collection-name 
           {:_id (ObjectId. id)}))
    (catch Exception e 
      (println (str "Exception: " (.getMessage e))))))

(defmulti find (fn [cls & args] cls))
(defmethod find :default [cls cond adapter]
  (let [collection-name (slugify-class cls)]
    (if (not (nil? (:_id cond)))
      (let [q (-find-model-by-id collection-name (.toString (:_id cond)))]
        (when (not (nil? q)) 
          (map (partial adapter cls) q)))
      (map (partial adapter cls)
           (mc/find-maps db-handle collection-name cond)))))

(defmulti update (fn [cls & args] cls))
(defmethod update :default [cls cond update-expr adapter]
  (let [collection-name (slugify-class cls)]
    (let [c (if (nil? (:_id cond)) 
              cond
              (assoc cond :_id (ObjectId. (.toString (:_id cond)))))]
      (try
        (adapter 
         cls
         (mc/find-and-modify
          db-handle
          collection-name
          c
          update-expr
          {:return-new true}))
              (catch Exception e
                (println (str "Exception: " (.getMessage e))))))))

(defmulti create (fn [cls & args] cls))
(defmethod create :default [cls params adapter]
  (let [collection-name (slugify-class cls)]
    (try
      (adapter
       cls
       (mc/insert-and-return 
        db-handle
        collection-name 
        params)))))
