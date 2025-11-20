# **Shelfify**

**Shelfify** is an Android app for **managing groceries** and **organizing recipes**.  
The app lets you add products manually or via barcode, track expiration dates, and use recipes based on the items you already have.  
All data is synchronized through a dedicated **Spring Boot API** and can be shared with others using **Datagroups**.

---

## ✨ Features ✨

### 🧺 Food Management
- ✅ Add products (name, EAN, expiration date)  
- ✅ Remove products  
- ✅ Automatically display expired products  

### 🍳 Recipes *(in progress)*
- 🔄 Store recipes with ingredients, preparation steps, and duration  
- 🔄 Filter buttons in the RecyclerView for quick and focused browsing  

### 📷 Barcode Scanner
- ✅ Integrated **Google ML Kit** for fast barcode-based product entry  

### 🔔 Notifications
- ✅ Reminders for expiring products  
- ✅ Configurable per **email**

### 👥 Datagroups
- ✅ Share your food inventory with multiple users  
- ✅ Invite users via email link directly to the app  
- 🔄 **Block invitations** *(in progress – Issue #5)*  
- ✅ Datagroup management and member overview
- 🔄 Rename products within Datagroups *(Issue #15)*  

### 📧 Email Integration
- ✅ Link your email address with your unique app ID  
- ✅ Token-based authentication for secure updates  
- ✅ Email verification implemented  

### 🔒 Security & API
- ✅ HTTPS support 
- ✅ Unique **App-ID** for reliable user data assignment  
- 🔄 Encrypt SharedPreferences for improved security *(Issue #19)*  

---

## ⚙️ API (ShelfifyApi)

Shelfify uses a dedicated **Spring Boot API** to handle all product, recipe, and user synchronization logic.  
API repository:  
👉 [ShelfifyApi Repository](https://github.com/Iloveschnitzel09/ShelfifyApi)
