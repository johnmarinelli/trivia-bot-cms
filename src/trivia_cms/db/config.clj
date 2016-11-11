(ns trivia-cms.db.config
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [environ.core :refer [env]])
  (:import [com.mongodb MongoOptions ServerAddress]))

(def database (env :database-name))
(def db-handle 
  (:db (mg/connect-via-uri 
        (clojure.string/join 
         "/" 
         (map env [:mongodb-uri-host :database-name])))))

(def quizzes-collection-name "quizzes")
(def questions-collection-name "questions")


