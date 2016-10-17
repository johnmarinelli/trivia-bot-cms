(ns trivia-cms.handler
  (:require [clojure.java.io]
            [clojure.data.json :as json]

            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.util.codec :as codec]

            [monger.core :as mg]
            [monger.collection :as mc]

            [trivia-cms.db :refer :all])

  (:use [stencil.core] ; html template rendering
        [ring.util.response :only [response not-found]] ; wrap json response
        [monger.operators] ; mongodb operators
        ))

(defn read-template [filepath]
  (slurp (clojure.java.io/resource filepath)))

; filepath is relative to `resources` directory
(defn render-html [filepath view-args]
  (let [html (read-template filepath)]
    (render-string html view-args)))

(defn index 
  [view-args] 
  {:page-type :index 
   :filepath "views/index.html" 
   :view-args view-args})

(defn quiz 
  [view-args] 
  {:page-type :quiz  
   :filepath "views/quizzes.html" 
   :view-args view-args})

(defn load-page 
  [view]
  (render-html 
   (:filepath view) 
   (:view-args view)))

(defn save-question! [quiz-name question]
  (mc/find-and-modify db-handle
                      quizzes-collection-name
                      {:quiz-name quiz-name} 
                      {$push {:questions question}}
                      {:return-new true}))

; this calls save-question behind the scenes
(defn save-quiz! [quiz]
  (let [questions (:questions quiz)]
    (let [quiz (first (mc/insert-and-return db-handle quizzes-collection-name (dissoc quiz :questions)))]
      (doseq [q questions]
        (save-question! (:quiz-name quiz) q)))
    quiz))

(defn get-quiz [name]
  (let [qs (mc/find-maps db-handle quizzes-collection-name {:quiz-name name})]
    (if (> (count qs) 0) 
      (update-in 
       (first qs)
       [:_id]
       (fn [qid]
         (.toString qid)))
      nil)))

(defn delete-question [quiz-name question-id]
  (mc/find-and-modify db-handle
                      quizzes-collection-name
                      (:quiz-name quiz-name)
                      ($pull {:_id question-id})
                      {:return-new true}))

(defn delete-quiz [name]
  (let [res (mc/remove db-handle quizzes-collection-name {:quiz-name name})] 
    (.getN res)))

(defroutes app-routes
  (GET "/" [request] (load-page (index {})))

  (GET "/quizzes" [request] (load-page (quiz {})))

  (POST "/quizzes/create" req
        (let [params (:params req)
              quiz-name (:quiz-name params)
              questions (or (:questions params) [])]
          (if (some nil? [params quiz-name])
            {:status 400 
             :body {:error-message  "Name is required when creating quizzes."} }
            (response (save-quiz! {:quiz-name quiz-name :questions questions})))))

  (DELETE "/quizzes/:name" [name]
          (let [num-deleted (delete-quiz name)]
            (if (> num-deleted 0)
              (response {:num-deleted num-deleted})
              (not-found 
               (json/write-str {:error-message (str "Quiz '" name "' not found.")})))))

  (GET "/quizzes/:name" [name]
       (let [quiz (get-quiz name)]
         (if (nil? quiz)
           (not-found {:error-message (str "Quiz '" name "' not found.")})
           (response quiz))))

  (DELETE "/quizzes/:quiz-name/questions/:id" [quiz-name id]
          (let [num-deleted (delete-question quiz-name id)]
            (if (> num-deleted 0)
              (response num-deleted)
              (not-found
               (json/write-str {:error-message "Question id '" id "' not found."})))))

  (POST "/quizzes/:quiz-name/questions/create" [quiz-name & params]
        (let [question-body (:question-body params)
              category (:category params)
              answer (:answer params)
              value (:value params)
              question {:question-body question-body
                        :category category
                        :answer answer
                        :value value}]
          (if (some nil? [name question-body category answer value])
            {:status 400
             :body {:error-message "All fields are required when creating questions."}}
            (response 
             (update-in
              (save-question! quiz-name question)
              [:_id]
              #(.toString %))))))


  (route/resources "/resources")
  (route/not-found "404"))

(def app
  (->
   app-routes
   (wrap-defaults api-defaults)
   (wrap-json-params)
   (wrap-json-response)))
