(ns status-im2.subs.communities
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [status-im.multiaccounts.core :as multiaccounts]
            [status-im.ui.screens.profile.visibility-status.utils :as visibility-status-utils]
            [status-im2.constants :as constants]
            [utils.i18n :as i18n]))

(re-frame/reg-sub
 :communities
 :<- [:raw-communities]
 :<- [:communities/enabled?]
 (fn [[raw-communities communities-enabled?]]
   (if communities-enabled?
     raw-communities
     [])))

(re-frame/reg-sub
 :communities/fetching-community
 :<- [:communities/resolve-community-info]
 (fn [info [_ id]]
   (get info id)))

(re-frame/reg-sub
 :communities/section-list
 :<- [:communities]
 (fn [communities]
   (->> (vals communities)
        (group-by (comp (fnil string/upper-case "") first :name))
        (sort-by (fn [[title]] title))
        (map (fn [[title data]]
               {:title title
                :data  data})))))

(re-frame/reg-sub
 :communities/community
 :<- [:communities]
 (fn [communities [_ id]]
   (get communities id)))

(re-frame/reg-sub
 :communities/community-chats
 :<- [:communities]
 (fn [communities [_ id]]
   (get-in communities [id :chats])))

(re-frame/reg-sub
 :communities/community-members
 :<- [:communities]
 (fn [communities [_ id]]
   (get-in communities [id :members])))

(re-frame/reg-sub
 :communities/sorted-community-members
 (fn [[_ community-id]]
   (let [contacts     (re-frame/subscribe [:contacts/contacts])
         multiaccount (re-frame/subscribe [:multiaccount])
         members      (re-frame/subscribe [:communities/community-members community-id])]
     [contacts multiaccount members]))
 (fn [[contacts multiaccount members] _]
   (let [names (reduce (fn [acc identity]
                         (let [me?     (= (:public-key multiaccount) identity)
                               contact (when-not me?
                                         (multiaccounts/contact-by-identity contacts identity))
                               name    (first (multiaccounts/contact-two-names-by-identity contact
                                                                                           multiaccount
                                                                                           identity))]
                           (assoc acc identity name)))
                       {}
                       (keys members))]
     (->> members
          (sort-by #(get names (get % 0)))
          (sort-by #(visibility-status-utils/visibility-status-order (get % 0)))))))

(re-frame/reg-sub
 :communities/featured-communities
 :<- [:communities/enabled?]
 :<- [:search/home-filter]
 :<- [:communities]
 (fn [[communities-enabled? search-filter communities]]
   (filterv
    (fn [{:keys [name id]}]
      (and (or communities-enabled?
               (= id constants/status-community-id))
           (or (empty? search-filter)
               (string/includes? (string/lower-case (str name)) search-filter))))
    (vals communities))))

(re-frame/reg-sub
 :communities/sorted-communities
 :<- [:communities]
 (fn [communities]
   (sort-by :name (vals communities))))

(re-frame/reg-sub
 :communities/communities
 :<- [:communities/enabled?]
 :<- [:search/home-filter]
 :<- [:communities]
 (fn [[communities-enabled? search-filter communities]]
   (filterv
    (fn [{:keys [name id]}]
      (and
       (or communities-enabled?
           (= id constants/status-community-id))
       (or (empty? search-filter)
           (string/includes? (string/lower-case (str name)) search-filter))))
    (vals communities))))

(re-frame/reg-sub
 :communities/community-ids
 :<- [:communities/communities]
 (fn [communities]
   (map :id communities)))

(re-frame/reg-sub
 :communities/community-ids-by-user-involvement
 :<- [:communities/communities]
 ;; Return communities splitted by level of user participation. Some communities user
 ;; already joined, to some of them join request sent and others were opened one day
 ;; and their data remained in app-db.
 ;; Result map has form: {:joined [id1, id2] :pending [id3, id5] :opened [id4]}"
 (fn [communities]
   (reduce (fn [acc community]
             (let [joined?    (:joined community)
                   requested? (pos? (:requested-to-join-at community))
                   id         (:id community)]
               (cond
                 joined?    (update acc :joined conj id)
                 requested? (update acc :pending conj id)
                 :else      (update acc :opened conj id))))
           {:joined [] :pending [] :opened []}
           communities)))

(defn community->home-item
  [community counts]
  {:name                  (:name community)
   :muted?                (:muted community)
   :unread-messages?      (pos? (:unviewed-messages-count counts))
   :unread-mentions-count (:unviewed-mentions-count counts)
   :community-icon        (:images community)})

(re-frame/reg-sub
 :communities/home-item
 (fn [[_ community-id]]
   [(re-frame/subscribe [:raw-communities])
    (re-frame/subscribe [:communities/unviewed-counts community-id])])
 (fn [[communities counts] [_ identity]]
   (community->home-item
    (get communities identity)
    counts)))

(re-frame/reg-sub
 :communities/edited-community
 :<- [:communities]
 :<- [:communities/community-id-input]
 (fn [[communities community-id]]
   (get communities community-id)))

(re-frame/reg-sub
 :communities/current-community
 :<- [:communities]
 :<- [:chats/current-raw-chat]
 (fn [[communities {:keys [community-id]}]]
   (get communities community-id)))

(re-frame/reg-sub
 :communities/unviewed-count
 (fn [[_ community-id]]
   [(re-frame/subscribe [:chats/by-community-id community-id])])
 (fn [[chats]]
   (reduce (fn [acc {:keys [unviewed-messages-count]}]
             (+ acc (or unviewed-messages-count 0)))
           0
           chats)))

(defn calculate-unviewed-counts
  [chats]
  (reduce (fn [acc {:keys [unviewed-mentions-count unviewed-messages-count]}]
            {:unviewed-messages-count (+ (:unviewed-messages-count acc) (or unviewed-messages-count 0))
             :unviewed-mentions-count (+ (:unviewed-mentions-count acc) (or unviewed-mentions-count 0))})
          {:unviewed-messages-count 0
           :unviewed-mentions-count 0}
          chats))

(re-frame/reg-sub
 :communities/unviewed-counts
 (fn [[_ community-id]]
   [(re-frame/subscribe [:chats/by-community-id community-id])])
 (fn [[chats]]
   (calculate-unviewed-counts chats)))

(re-frame/reg-sub
 :communities/requests-to-join-for-community
 :<- [:communities/requests-to-join]
 (fn [requests [_ community-id]]
   (->>
    (get requests community-id {})
    vals
    (filter (fn [{:keys [state]}]
              (= state constants/request-to-join-pending-state))))))

(re-frame/reg-sub
 :community/categories
 (fn [[_ community-id]]
   [(re-frame/subscribe [:communities/community community-id])])
 (fn [[{:keys [categories]}] _]
   categories))

(re-frame/reg-sub
 :communities/sorted-categories
 :<- [:communities]
 (fn [communities [_ id]]
   (->> (get-in communities [id :categories])
        (map #(assoc (get % 1) :community-id id))
        (sort-by :position)
        (into []))))


(def token-images
  {"KNC"  (js/require "../resources/images/tokens/mainnet/KNC.png")
   "MANA" (js/require "../resources/images/tokens/mainnet/MANA.png")
   "RARE" (js/require "../resources/images/tokens/mainnet/RARE.png")
   "ETH"  (js/require "../resources/images/tokens/mainnet/ETH.png")
   "DAI"  (js/require "../resources/images/tokens/mainnet/DAI.png")})

(defn token-image
  [token]
  (get token-images token))

(defn enrich-gate-for-ui
  [{:keys [token] :as gate}]
  (assoc gate :token-img-src (token-image token) :is-sufficient? false))

(defn enrich-gates-vector-for-ui
  [gates]
  (vec (map enrich-gate-for-ui gates)))

(defn enrich-gates-for-ui
  [gates]
  (vec (map (fn [v]
              (if (vector? v)
                (enrich-gates-vector-for-ui v)
                (enrich-gate-for-ui v)))
            gates)))

(re-frame/reg-sub
 :communities/categorized-channels
 (fn [[_ community-id]]
   [(re-frame/subscribe [:communities/community community-id])
    (re-frame/subscribe [:chats/chats])])
 (fn [[{:keys [joined categories chats]} full-chats-data] [_ community-id]]
   (reduce
    (fn [acc [_ {:keys [name categoryID id emoji can-post? gates]}]]
      (let [category                                                  (keyword
                                                                       (get-in categories
                                                                               [categoryID :name]
                                                                               (i18n/label :t/none)))
            {:keys [unviewed-messages-count unviewed-mentions-count]} (get full-chats-data
                                                                           (str community-id id))]
        (update acc
                category
                #(vec (conj %1 %2))
                {:name             name
                 :emoji            emoji
                 :gates            (merge
                                    (when (contains? gates :read)
                                      {:read (enrich-gates-for-ui (:read gates))})
                                    (when (contains? gates :write)
                                      {:write (enrich-gates-for-ui (:write gates))}))
                 :unread-messages? (pos? unviewed-messages-count)
                 :mentions-count   (or unviewed-mentions-count 0)
                 :locked?          (or (not joined) (not can-post?))
                 :id               id})))
    {}
    chats)))

(re-frame/reg-sub
 :communities/users
 :<- [:communities]
 (fn [_ [_ _]]
   [{:full-name "Alicia K"}
    {:full-name "Marcus C"}
    {:full-name "MNO PQR"}
    {:full-name "STU VWX"}]))

(re-frame/reg-sub
 :community/join-gates
 (fn [[_ community-id]]
   [(re-frame/subscribe [:communities/community community-id])])
 (fn [[{:keys [gates]}] _]
   (when gates
     {:join (enrich-gates-for-ui (gates :join))})))

(defn icons-for-permission-tag
  [gates]
  (vec (map-indexed (fn [i {:keys [token]}]
                      {:id i :token-icon (token-image token)})
                    gates)))

(re-frame/reg-sub
 :community/permission-tag-tokens
 (fn [[_ community-id]]
   [(re-frame/subscribe [:communities/community community-id])])
 (fn [[{:keys [gates]}] _]
   (when gates
     (let [join-gates (gates :join)
           first-item (first join-gates)]
       (if (vector? first-item)
         (vec (map-indexed (fn [i v]
                             {:id    i
                              :group (icons-for-permission-tag v)})
                           join-gates))
         [{:id    0
           :group (icons-for-permission-tag join-gates)}])))))
