(ns quo2.components.colors.color.style
  (:require [quo2.foundations.colors :as colors]))

(defn color-button-common
  [window-width]
  {:width             (if window-width
                        (/ window-width 7.8125)
                        48)
   :height            (if window-width
                        (/ window-width 7.8125)
                        48)
   :border-width      4
   :margin-horizontal (/ window-width 93.75)
   :border-radius     24
   :transform         [{:rotate "45deg"}]
   :border-color      :transparent})

(defn color-button
  ([color selected?]
   (color-button color selected? nil nil))
  ([color selected? idx window-width]
   (merge (color-button-common window-width)
          (when selected?
            {:border-top-color    (colors/alpha color 0.4)
             :border-end-color    (colors/alpha color 0.4)
             :border-bottom-color (colors/alpha color 0.2)
             :border-start-color  (colors/alpha color 0.2)}
            (when (zero? idx)
              {:margin-left  -4
               :margin-right (/ window-width 93.75)})))))

(defn color-circle
  ([color border?]
   (color-circle color border? nil))
  ([color border? window-width]
   {:width            (if window-width
                        (/ window-width 9.375)
                        40)
    :height           (if window-width
                        (/ window-width 9.375)
                        40)
    :transform        [{:rotate "-45deg"}]
    :background-color color
    :justify-content  :center
    :align-items      :center
    :border-color     color
    :border-width     (if border? 2 0)
    :overflow         :hidden
    :border-radius    100}))

(defn feng-shui
  [theme]
  {:width            40
   :height           40
   :transform        [{:rotate "45deg"}]
   :overflow         :hidden
   :border-color     (colors/theme-colors colors/neutral-100 colors/white theme)
   :border-width     2
   :background-color (colors/theme-colors colors/neutral-100 colors/white theme)
   :border-radius    20})

(defn left-half
  [theme]
  {:flex             1
   :background-color (colors/theme-colors colors/white colors/neutral-100 theme)})

(defn right-half
  [theme]
  {:flex             1
   :background-color (colors/theme-colors colors/neutral-100 colors/white theme)})
