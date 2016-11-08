(ns trivia-cms.api.public-api)

; Since we have a public API for quizzes,
; we define an interface between the app and the public facing API.
(defprotocol IPublicAPI
  (serialize [this]))
