(ns trivia-cms.db
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [environ.core :refer [env]])
  (:import [com.mongodb MongoOptions ServerAddress]))

(def conn 
  (mg/connect 
   {:host 
    (or (get (System/getenv) "MONGO_TRIVIA_BOT_HOST") "127.0.0.1") 
    :port 
    (Integer. (or (get (System/getenv) "MONGO_TRIVIA_BOT_PORT") "27017"))}))

(def database (env :database-name))
(def db-handle (mg/get-db conn database))
(def quizzes-collection-name "quizzes")
