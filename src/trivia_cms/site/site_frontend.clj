(ns trivia-cms.site.site-frontend
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]

            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]

            [trivia-cms.site.views.layout :as layout]
            [trivia-cms.site.views.content :as content])
  (:use [ring.util.response :only [response not-found]]))

(def site-title "LTCLA Trivia")

(defroutes site-routes
  (GET "/" _
       (layout/application site-title (content/home)))
  (GET "/quizzes" [request] 
       (layout/application site-title (content/all-quizzes)))
  (GET "/quizzes/create" req
       (layout/application site-title (content/create-quiz)))
  (route/not-found "404"))

(def site
  (handler/site (-> site-routes
                    (wrap-defaults site-defaults))))
