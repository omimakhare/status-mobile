(ns status-im2.contexts.onboarding.enable-biometrics.style
  (:require [quo2.foundations.colors :as colors]))

(def default-margin 20)

(defn page-container
  [insets]
  {:flex             1
   :padding-top      (:top insets)
   :background-color colors/neutral-80-opa-80-blur})

(def page-illustration
  {:flex 1
   ;;  :background-color  colors/danger-50
   ;;  :margin-horizontal default-margin
  })

(defn buttons
  [insets]
  {:margin        default-margin
   :margin-bottom (+ default-margin (:bottom insets))})
