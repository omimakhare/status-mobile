(ns status-im2.contexts.wallet.scan-account.view
  (:require [clojure.string :as string]
            [oops.core :as oops]
            [quo2.core :as quo]
            [quo2.foundations.colors :as colors]
            [react-native.blur :as blur]
            [react-native.camera-kit :as camera-kit]
            [react-native.core :as rn]
            [react-native.hole-view :as hole-view]
            [react-native.permissions :as permissions]
            [react-native.safe-area :as safe-area]
            [reagent.core :as reagent]
            [status-im2.common.device-permissions :as device-permissions]
            [status-im2.contexts.wallet.scan-account.style :as style]
            [utils.address :as address-utils]
            [utils.debounce :as debounce]
            [utils.i18n :as i18n]
            [utils.re-frame :as rf]
            [utils.transforms :as transforms]
            [quo2.theme :as quo.theme]))

(defonce camera-permission-granted? (reagent/atom false))

(defn- header
  [{:keys [title]}]
  [:<>
   [rn/view {:style style/header-container}
    [quo/button
     {:icon-only?          true
      :type                :grey
      :background          :blur
      :size                32
      :accessibility-label :close-sign-in-by-syncing
      :on-press            #(rf/dispatch [:navigate-back])}
     :i/arrow-left]]
   [quo/text
    {:size   :heading-1
     :weight :semi-bold
     :style  style/header-text}
    title]
   [quo/text
    {:size   :paragraph-1
     :weight :regular
     :style  style/header-sub-text}
    "Scan an account QR code"]])

(defn get-labels-and-on-press-method
  []
  {:title-label-key       :t/enable-access-to-camera
   :description-label-key :t/to-scan-a-qr-enable-your-camera
   :button-icon           :i/camera
   :button-label          :t/enable-camera
   :accessibility-label   :request-camera-permission
   :on-press              (fn []
                            (device-permissions/camera #(reset! camera-permission-granted? true)))})

(defn- camera-permission-view
  []
  (let [{:keys [title-label-key
                description-label-key
                button-icon
                button-label
                accessibility-label
                on-press]} (get-labels-and-on-press-method)]
    [rn/view {:style style/camera-permission-container}
     [quo/text
      {:size   :paragraph-1
       :weight :medium
       :style  style/enable-camera-access-header}
      (i18n/label title-label-key)]
     [quo/text
      {:size   :paragraph-2
       :weight :regular
       :style  style/enable-camera-access-sub-text}
      (i18n/label description-label-key)]
     [quo/button
      {:icon-left           button-icon
       :type                :primary
       :size                32
       :accessibility-label accessibility-label
       :customization-color :blue
       :on-press            on-press}
      (i18n/label button-label)]]))

(defn- qr-scan-hole-area
  [qr-view-finder]
  [rn/view
   {:style     style/qr-view-finder
    :on-layout (fn [event]
                 (let [layout      (transforms/js->clj (oops/oget event "nativeEvent.layout"))
                       view-finder (assoc layout :height (:width layout))]
                   (reset! qr-view-finder view-finder)))}])

(defn- white-border
  [corner]
  (let [border-styles (style/white-border corner)]
    [rn/view
     [rn/view {:style (border-styles :border)}]
     [rn/view {:style (border-styles :tip-1)}]
     [rn/view {:style (border-styles :tip-2)}]]))

(defn- white-square
  [layout-size]
  [rn/view {:style (style/qr-view-finder-container layout-size)}
   [rn/view {:style style/view-finder-border-container}
    [white-border :top-left]
    [white-border :top-right]]
   [rn/view {:style style/view-finder-border-container}
    [white-border :bottom-left]
    [white-border :bottom-right]]])

(defn- viewfinder
  [qr-view-finder]
  (let [layout-size (+ (:width qr-view-finder) 2)]
    [rn/view {:style (style/viewfinder-container qr-view-finder)}
     [white-square layout-size]
     [quo/text
      {:size   :paragraph-2
       :weight :regular
       :style  style/viewfinder-text}
      (i18n/label :t/ensure-qr-code-is-in-focus-to-scan)]]))

(defn- scan-qr-code-tab
  [qr-view-finder]
  (if (and @camera-permission-granted?
           (boolean (not-empty qr-view-finder)))
    [viewfinder qr-view-finder]
    [camera-permission-view]))

(defn- check-qr-code-and-navigate
  [{:keys [event on-success-scan on-failed-scan]}]
  (let [scanned-address (string/trim (oops/oget event "nativeEvent.codeStringValue"))
        valid-address?  (address-utils/address? scanned-address)]
    ;; debounce-and-dispatch used because the QR code scanner performs callbacks too fast
    (if valid-address?
      (do
        (on-success-scan)
        (debounce/debounce-and-dispatch
         [:wallet-2/scan-address-success scanned-address]
         300))
      (do
        (on-failed-scan)
        (debounce/debounce-and-dispatch
         [:toasts/upsert
          {:icon       :i/info
           :icon-color colors/danger-50
           :theme      :dark
           :text       (i18n/label :t/oops-this-qr-does-not-contain-an-address)}]
         300)))))

(defn- render-camera
  [{:keys [torch-mode qr-view-finder scan-code? set-qr-code-succeeded set-rescan-timeout]}]
  [:<>
   [rn/view {:style style/camera-container}
    [camera-kit/camera
     {:style        style/camera-style
      :camera-type  camera-kit/camera-type-back
      :zoom-mode    :off
      :torch-mode   torch-mode
      :scan-barcode true
      :on-read-code #(when scan-code?
                       (check-qr-code-and-navigate {:event           %
                                                    :on-success-scan set-qr-code-succeeded
                                                    :on-failed-scan  set-rescan-timeout}))}]]
   [hole-view/hole-view
    {:style style/hole
     :holes [(assoc qr-view-finder :borderRadius 16)]}
    [blur/view
     {:style            style/absolute-fill
      :blur-amount      10
      :blur-type        :transparent
      :overlay-color    colors/neutral-80-opa-80
      :background-color colors/neutral-80-opa-80}]]])

(defn- set-listener-torch-off-on-app-inactive
  [torch-atm]
  (let [set-torch-off-fn   #(when (not= % "active") (reset! torch-atm false))
        app-state-listener (.addEventListener rn/app-state "change" set-torch-off-fn)]
    #(.remove app-state-listener)))

(defn f-view-internal
  []
  (let [insets             (safe-area/get-insets)
        qr-code-succeed?   (reagent/atom false)
        qr-view-finder     (reagent/atom {})
        torch?             (reagent/atom false)
        scan-code?         (reagent/atom true)
        set-rescan-timeout (fn []
                             (reset! scan-code? false)
                             (js/setTimeout #(reset! scan-code? true) 3000))]
    (fn []
      (let [torch-mode            (if @torch? :on :off)
            flashlight-icon       (if @torch? :i/flashlight-on :i/flashlight-off)
            show-camera?          (and @camera-permission-granted?
                                       (boolean (not-empty @qr-view-finder)))
            camera-ready-to-scan? (and show-camera?
                                       (not @qr-code-succeed?))]
        (rn/use-effect
         #(set-listener-torch-off-on-app-inactive torch?))

        (rn/use-effect
         (fn []
           (when-not @camera-permission-granted?
             (permissions/permission-granted? :camera
                                              #(reset! camera-permission-granted? %)
                                              #(reset! camera-permission-granted? false)))))
        [:<>
         [rn/view {:style style/background}]
         (when camera-ready-to-scan?
           [render-camera
            {:torch-mode            torch-mode
             :qr-view-finder        @qr-view-finder
             :scan-code?            @scan-code?
             :set-qr-code-succeeded #(rf/dispatch [:navigate-back])
             :set-rescan-timeout    set-rescan-timeout}])
         [rn/view {:style (style/root-container (:top insets))}
          [header {:title (i18n/label :t/scan-qr)}]
          (when (empty? @qr-view-finder)
            [:<>
             [rn/view {:style style/scan-qr-code-container}]
             [qr-scan-hole-area qr-view-finder]])
          [scan-qr-code-tab @qr-view-finder]
          [rn/view {:style style/flex-spacer}]
          (when show-camera?
            [quo/button
             {:icon-only?          true
              :type                :grey
              :background          :blur
              :size                style/flash-button-size
              :accessibility-label :camera-flash
              :container-style     (style/camera-flash-button @qr-view-finder)
              :on-press            #(swap! torch? not)}
             flashlight-icon])]]))))

(defn view-internal
  []
  [:f> f-view-internal])

(def view (quo.theme/with-theme view-internal))
