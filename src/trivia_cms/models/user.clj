(ns trivia-cms.models.user
  (:require [trivia-cms.errors.api-error :refer [api-error]]
            [trivia-cms.db.config :refer :all]
            [monger.core :as mg]
            [monger.collection :as mc]
            [buddy.hashers :as hashers])
  (:use monger.operators)
  (:import [org.bson.types ObjectId]))

(def collection-name "users")

(defn find-user [username]
  (first (mc/find-maps db-handle collection-name {:username username})))

(defn check-user-password [username password]
  (let [user (find-user username)]
    (if (not (nil? user))
      (when (hashers/check password (:password-hash user))
        user)
      false)))

(defn create-user! [user]
  (let [password (:password user)]
    (-> user
        (assoc  :password-hash (hashers/encrypt password))
        (dissoc :password)
        (->> (mc/insert-and-return db-handle collection-name)))))


