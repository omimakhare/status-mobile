(ns status-im2.contexts.wallet.home.view
  (:require
    [react-native.core :as rn]
    [quo2.core :as quo]
    [react-native.safe-area :as safe-area]
    [reagent.core :as reagent]
    [status-im2.common.home.top-nav.view :as common.top-nav]
    [status-im2.contexts.wallet.home.style :as style]
    [utils.i18n :as i18n]
    [clojure.string :as string]
    [utils.re-frame :as rf]
    [status-im2.contexts.wallet.common.temp :as temp]))

(defn new-account
  []
  [quo/action-drawer
   [[{:icon                :i/add
      :accessibility-label :start-a-new-chat
      :label               (i18n/label :t/add-account)
      :sub-label           (i18n/label :t/add-account-description)
      :on-press            #(rf/dispatch [:navigate-to :wallet-create-account])}
     {:icon                :i/reveal
      :accessibility-label :add-a-contact
      :label               (i18n/label :t/add-address)
      :sub-label           (i18n/label :t/add-address-description)
      :on-press            #(rf/dispatch [:navigate-to :wallet-address-watch])
      :add-divider?        true}]]])

(def add-account-placeholder
  {:customization-color :blue
   :on-press            #(rf/dispatch [:show-bottom-sheet {:content new-account}])
   :type                :add-account})

(def tabs-data
  [{:id :assets :label (i18n/label :t/assets) :accessibility-label :assets-tab}
   {:id :collectibles :label (i18n/label :t/collectibles) :accessibility-label :collectibles-tab}
   {:id :activity :label (i18n/label :t/activity) :accessibility-label :activity-tab}])

(defn calculate-raw-balance 
  [rawBalance decimals]
  (/ (js/parseInt rawBalance) (Math/pow 10 (js/parseInt decimals))))

(defn calculate-balance
  [{:keys [address]}]
  (let [tokens (rf/sub [:wallet-2/tokens])
        token  (get tokens (keyword (string/lower-case address)))
        total-values (atom 0)]
    (doseq [item token]
      (let [total-value-per-token (atom 0)]
        (doseq [balances (vals (:balancesPerChain item))]
          (reset! total-value-per-token (+ (calculate-raw-balance (:rawBalance balances) (:decimals item)) @total-value-per-token)))
        (reset! total-values (+ (* @total-value-per-token (get-in item [:marketValuesPerCurrency :USD :price])) @total-values))))
    (.toFixed @total-values 2)))

(defn refactor-data
  []
  (let [accounts (rf/sub [:profile/wallet-accounts])
        loading? (rf/sub [:wallet-2/tokens-loading?])
        refactored-accounts (mapv (fn [account]
                                   (merge account {:type :empty
                                                   :customization-color :blue
                                                   :on-press            #(rf/dispatch [:navigate-to :wallet-accounts])
                                                   :loading? loading?
                                                   :balance (str "$" (calculate-balance account))})) 
                                  accounts)] 
        (merge refactored-accounts add-account-placeholder)))

(defn reagent-render
  []
  (let [top          (safe-area/get-top)
        selected-tab (reagent/atom (:id (first tabs-data)))]
    (fn []
      [rn/view
       {:style {:margin-top top
                :flex       1}}
       [common.top-nav/view]
       [rn/view {:style style/overview-container}
        [quo/wallet-overview temp/wallet-overview-state]]
       [rn/pressable
        {:on-long-press #(rf/dispatch [:show-bottom-sheet {:content temp/wallet-temporary-navigation}])}
        [quo/wallet-graph {:time-frame :empty}]]
       [rn/flat-list
        {:style      style/accounts-list
         :data       (refactor-data)
         :horizontal true
         :separator  [rn/view {:style {:width 12}}]
         :render-fn  quo/account-card}]
       [quo/tabs
        {:style          style/tabs
         :size           32
         :default-active @selected-tab
         :data           tabs-data
         :on-change      #(reset! selected-tab %)}]
       (case @selected-tab
         :assets       [rn/flat-list
                        {:render-fn               quo/token-value
                         :data                    temp/tokens
                         :key                     :assets-list
                         :content-container-style {:padding-horizontal 8}}]
         :collectibles (if temp/collectible-details
                         [rn/flat-list
                          {:render-fn               (fn [item]
                                                      [quo/collectible
                                                       {:images   [(:image item)]
                                                        :on-press #(rf/dispatch [:navigate-to
                                                                                 :wallet-collectible])}])
                           :data                    temp/collectibles
                           :key                     :collectibles-list
                           :key-fn                  :id
                           :num-columns             2
                           :content-container-style {:padding-horizontal 8}}]
                         [quo/empty-state
                          {:title           (i18n/label :t/no-collectibles)
                           :description     (i18n/label :t/no-collectibles-description)
                           :placeholder?    true
                           :container-style style/empty-container-style}])
         [quo/empty-state
          {:title           (i18n/label :t/no-activity)
           :description     (i18n/label :t/empty-tab-description)
           :placeholder?    true
           :container-style style/empty-container-style}])])))

(defn view
  []
  (reagent/create-class
   (let [accounts (rf/sub [:profile/wallet-accounts])]
     {:component-did-mount #(rf/dispatch [:wallet-2/get-wallet-tokens accounts])
      :reagent-render reagent-render})))
