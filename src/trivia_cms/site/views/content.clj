(ns trivia-cms.site.views.content
  (:require [trivia-cms.api.api-backend :as api])
  (:use [hiccup.form]
        [hiccup.core]
        [hiccup.element :only (link-to)]))

(defn home []
  [:div "Home"])

(defn create-quiz []
  (form-to [:post "/quizzes/create"]
            (text-field {:class "form-control" :placeholder "My Quiz"} :quiz-name)
            (submit-button "Create New Quiz")))

(defn all-quizzes []
  (let [quizzes (api/get-quizzes)]
    (html [:ul
           (for [quiz quizzes]
             [:li 
              [:div.list-item 
               [:h1 (h (:quiz-name quiz))]]])])))
