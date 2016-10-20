(ns trivia-cms.views.content
  (:use [hiccup.form]
        [hiccup.element :only (link-to)]))

(defn create-quiz []
  (form-to [:post "/quizzes/create"]
            (text-field {:class "form-control" :placeholder "My Quiz"} :quiz-name)
            (submit-button "Create New Quiz")))
