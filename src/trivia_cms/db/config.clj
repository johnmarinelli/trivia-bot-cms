(ns trivia-cms.db.config
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [environ.core :refer [env]])
  (:import [com.mongodb MongoOptions ServerAddress]))

(def db-handle 
  (:db 
   (mg/connect-via-uri 
    (clojure.string/join 
     "/" 
     (map env [:mongodb-uri-host :database-name])))))

(def database (env :database-name))
(def db-handle (if (nil? (System/getenv "MONGODB_URI")) 
                 (mg/get-db conn database)
                 (:db (mg/connect-via-uri (System/getenv "MONGODB_URI")))))


