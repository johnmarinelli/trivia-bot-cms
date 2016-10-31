(ns trivia-cms.db.config
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [environ.core :refer [env]])
  (:import [com.mongodb MongoOptions ServerAddress]))

(def conn 
  (if (nil? (System/getenv "MONGODB_URI")) 
    (mg/connect 
     {:host 
      (or (get (System/getenv) "MONGO_TRIVIA_BOT_HOST") "127.0.0.1") 
      :port 
      (Integer. (or (get (System/getenv) "MONGO_TRIVIA_BOT_PORT") "27017"))})
    (mg/connect 
     (:conn (mg/connect-via-uri (System/getenv "MONGODB_URI"))))))

(def database (env :database-name))
(def db-handle (if (nil? (System/getenv "MONGODB_URI")) 
                 (mg/get-db conn database)
                 (:db (mg/connect-via-uri (System/getenv "MONGODB_URI")))))
(def quizzes-collection-name "quizzes")
(def questions-collection-name "questions")


