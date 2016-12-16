(ns trivia-cms.models.user
  (:require [trivia-cms.errors.api-error :refer [api-error]]
            [trivia-cms.db.config :refer :all]
            [monger.core :as mg]
            [monger.collection :as mc]
            [buddy.hashers :as hashers])
  (:use monger.operators)
  (:import [org.bson.types ObjectId]))

; TODO: refactor models to use {:_id :name} destructure pattern
(def collection-name "users")

(defn find-user [username]
  (first (mc/find-maps db-handle collection-name {:username username})))

(defn find-user-by-id [id]
  (first (mc/find-maps db-handle collection-name {:_id (ObjectId. id)})))

(defn check-user-password [username password]
  (let [user (find-user username)]
    (if (not (nil? user))
      (when (hashers/check password (:password-hash user))
        user)
      false)))

(defn set-token [{id :_id username :username} token]
  (println id)
  (let [cond (if (nil? id) {:username username} {:_id (ObjectId. id)})]
    (mc/find-and-modify db-handle 
                        collection-name 
                        cond
                        {:$set {:token token} }
                        {:return-new true})))

(defn get-token [id]
  (let [user (first (mc/find-maps db-handle collection-name {:_id (ObjectId. id)}))]
    (:token user)))

(defn remove-token [username]
  (mc/find-and-modify db-handle
                      collection-name
                      {:username username}
                      {:$set {:token ""}}
                      {:return-new true}))

(defn set-modified-at [{id :_id username :username}]
  (let [cond (if (nil? id) {:username username} {:_id (ObjectId. id)})]
      (mc/find-and-modify db-handle
                      collection-name
                      cond
                      {:$set {:modified_at (System/currentTimeMillis)}}
                      {:return-new true})))

(defn create-user! [user]
  (let [password (:password user)]
    (-> user
        (assoc  :password-hash (hashers/encrypt password))
        (dissoc :password)
        (->> (mc/insert-and-return db-handle collection-name)))))


