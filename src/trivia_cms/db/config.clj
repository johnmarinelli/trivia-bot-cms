(ns trivia-cms.db.config
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [environ.core :refer [env]])
  (:import [com.mongodb MongoOptions ServerAddress]))

(println "ENVIRONTMENT:")
(println (env :mongodb-uri-host))
(println (env :database-name))
(def db-handle 
  (:db 
   (mg/connect-via-uri 
    (clojure.string/join 
     "/" 
     (map env [:mongodb-uri-host :database-name])))))

(comment "mongodb://heroku_np83q2zj:9o8vhhj4nchpddr3ancrh0hlen@ds025389.mlab.com:25389/heroku_np83q2zj")
(comment)

(def quizzes-collection-name "quizzes")
(def questions-collection-name "questions")


