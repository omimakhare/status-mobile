(ns status-im2.contexts.shell.jump-to.shared-values
  (:require [quo2.foundations.colors :as colors]
            [react-native.safe-area :as safe-area]
            [react-native.reanimated :as reanimated]
            [utils.worklets.shell :as worklets.shell]
            [status-im2.contexts.shell.jump-to.utils :as utils]
            [status-im2.contexts.shell.jump-to.state :as state]
            [status-im2.contexts.shell.jump-to.constants :as shell.constants]))

(defn calculate-home-stack-position
  [{:keys [width height]}]
  (let [bottom-nav-tab-width   shell.constants/bottom-tab-width
        minimize-scale         (/ bottom-nav-tab-width width)
        empty-space-half-scale (/ (- 1 minimize-scale) 2)
        left-margin            (/ (- width (* 4 bottom-nav-tab-width)) 2)
        left-empty-space       (* empty-space-half-scale width)
        top-empty-space        (* empty-space-half-scale
                                  (- height (utils/bottom-tabs-container-height)))]
    {:left  (reduce
             (fn [acc stack-id]
               (assoc acc
                      stack-id
                      (+ (- left-margin left-empty-space)
                         (* (.indexOf shell.constants/stacks-ids stack-id)
                            bottom-nav-tab-width))))
             {:none 0}
             shell.constants/stacks-ids)
     :top   (+ top-empty-space (utils/bottom-tabs-container-height))
     :scale minimize-scale}))

(defn stacks-and-bottom-tabs-derived-values
  [{:keys [selected-stack-id home-stack-state]}]
  (let [pass-through (reanimated/use-shared-value false)]
    (reduce
     (fn [acc id]
       (let [tabs-icon-color-keyword (get shell.constants/tabs-icon-color-keywords id)
             stack-opacity-keyword   (get shell.constants/stacks-opacity-keywords id)
             stack-z-index-keyword   (get shell.constants/stacks-z-index-keywords id)]
         (assoc
          acc
          stack-opacity-keyword
          (worklets.shell/stack-opacity (name id) selected-stack-id)
          stack-z-index-keyword
          (worklets.shell/stack-z-index (name id) selected-stack-id)
          tabs-icon-color-keyword
          (worklets.shell/bottom-tab-icon-color
           (name id)
           selected-stack-id
           home-stack-state
           pass-through
           colors/white
           colors/neutral-50
           colors/white-opa-40))))
     {:bottom-tabs-height (worklets.shell/bottom-tabs-height
                           home-stack-state
                           (utils/bottom-tabs-container-height)
                           (utils/bottom-tabs-extended-container-height))
      :pass-through       pass-through}
     shell.constants/stacks-ids)))

(defn home-stack-derived-values
  [{:keys [selected-stack-id home-stack-state]} dimensions]
  (let [home-stack-position     (calculate-home-stack-position dimensions)
        animate-home-stack-left (reanimated/use-shared-value (not (utils/home-stack-open?)))]
    {:animate-home-stack-left  animate-home-stack-left
     :home-stack-left          (worklets.shell/home-stack-left
                                selected-stack-id
                                animate-home-stack-left
                                home-stack-state
                                (clj->js (:left home-stack-position)))
     :home-stack-top           (worklets.shell/home-stack-top
                                home-stack-state
                                (:top home-stack-position))
     :home-stack-opacity       (worklets.shell/home-stack-opacity home-stack-state)
     :home-stack-pointer       (worklets.shell/home-stack-pointer home-stack-state)
     :home-stack-border-radius (worklets.shell/home-stack-border-radius home-stack-state)
     :home-stack-scale         (worklets.shell/home-stack-scale
                                home-stack-state
                                (:scale home-stack-position))}))

(defn floating-screen-derived-values
  [screen-id {:keys [width height]} switcher-card-left-position switcher-card-top-position
   home-list-floating-screen-width shared-data]
  (let [screen-state (reanimated/use-shared-value
                      (if (utils/floating-screen-open? screen-id)
                        shell.constants/open-screen-without-animation
                        shell.constants/close-screen-without-animation))]
    {:screen-state         screen-state
     :screen-left          (worklets.shell/floating-screen-left
                            screen-state
                            width
                            switcher-card-left-position
                            shell.constants/home-list-item-left-position)
     :screen-top           (worklets.shell/floating-screen-top screen-state switcher-card-top-position shared-data)
     :screen-z-index       (worklets.shell/floating-screen-z-index screen-state)
     :screen-width         (worklets.shell/floating-screen-width screen-state
                                                                 width
                                                                 shell.constants/switcher-card-size
                                                                 home-list-floating-screen-width)
     :screen-border-radius (worklets.shell/floating-screen-border-radius
                            screen-state
                            shell.constants/switcher-card-border-radius
                            shell.constants/home-list-item-border-radius)
     :screen-height        (worklets.shell/floating-screen-height
                            screen-state
                            height
                            shell.constants/switcher-card-size
                            shell.constants/home-list-item-height)}))

(defn calculate-and-set-shared-values
  []
  (let [{:keys [width] :as dimensions} (utils/dimensions)
        switcher-card-left-position (/ (- width (* 2 shell.constants/switcher-card-size)) 3)
        switcher-card-top-position (+ (safe-area/get-top) 120)
        shared-data (reanimated/use-shared-value (clj->js {}))
        home-list-floating-screen-width (* width
                                           (/ shell.constants/home-list-community-icon-size
                                              shell.constants/community-overview-community-icon-size))
        shared-values
        {:shared-data       shared-data
         :selected-stack-id (reanimated/use-shared-value
                             (name (or @state/selected-stack-id :communities-stack)))
         :home-stack-state  (reanimated/use-shared-value @state/home-stack-state)}]
    ;; Whenever shell stack is created, calculate shared values function is called
    ;; Means On login and on UI reloading (like changing theme)
    ;; So we are also resetting bottom tabs here (disabling loading of unselected tabs),
    ;; for Speed up UI reloading
    (utils/reset-bottom-tabs)
    (reset!
      state/shared-values-atom
      (merge
       shared-values
       (stacks-and-bottom-tabs-derived-values shared-values)
       (home-stack-derived-values shared-values dimensions)
       {shell.constants/community-screen (floating-screen-derived-values
                                          shell.constants/community-screen
                                          dimensions
                                          switcher-card-left-position
                                          switcher-card-top-position
                                          home-list-floating-screen-width
                                          shared-data)
        shell.constants/chat-screen      (floating-screen-derived-values
                                          shell.constants/chat-screen
                                          dimensions
                                          switcher-card-left-position
                                          switcher-card-top-position
                                          home-list-floating-screen-width
                                          shared-data)}))
    @state/shared-values-atom))
