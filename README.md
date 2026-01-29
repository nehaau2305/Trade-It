# UGA Trade It Android App
Android application that creates a platform for users to post & purchase items. The app utilizes a database for user authentication, item retrieval, & category retrieval.

## Features
- Account creation with a name, email (in valid email format), & password
- User logs in with the correct email & password which is verified by Google Firebase Authentication
- Home page allows user to view all items posted by all users with the current status of available
- Users can add an item by specifying a category, item name, price (can be free), & description
- Users can create categories & also edit or delete the categories they have created if there are not available items under the category already
- My Items page will allow users to view, edit, & delete the items or categories they created
- Transactions page dispalys the pending & completed transactions of the user
- Users can confirm the purchase & sale of items
- Search functionality enables browsing by category or querying for an item by name
- Sort functionality to order items from newest to oldest post time or alphabetically
- Google Firebase Realtime Database allows numerous users to use the application at the same time & view changes immediately
- Applications is compatible for both portrait & landscape orientations 

## Technologies Used
- Language: Java
- Database: Google Firebase Realtime Database
- IDE: Android Studio
- Build System: Gradle
- UI Layouts: XML
- Testing Environment: Android Emulator

## Screenshots
#### Initial Screen
<a href="images/initialScreen.png">
  <img src="images/initialScreen.png" width="200">
</a>
<a href="images/initialScreenLdscp.png">
  <img src="images/initialScreenLdscp.png" height="200">
</a>

#### Account Registration Screen
<a href="images/register.png">
  <img src="images/register.png" width="200">
</a>
<a href="images/registerLdscp.png">
  <img src="images/registerLdscp.png" height="200">
</a>

#### Login Screen
<a href="images/login.png">
  <img src="images/login.png" width="200">
</a>
<a href="images/loginLdscp.png">
  <img src="images/loginLdscp.png" height="200">
</a>

#### Authenticated Home Screen
<a href="images/home.png">
  <img src="images/home.png" width="200">
</a>
<a href="images/homeLdscp.png">
  <img src="images/homeLdscp.png" height="200">
</a>

#### My Items Screen
<a href="images/myItems.png">
  <img src="images/myItems.png" width="200">
</a>
<a href="images/myItemsLdscp.png">
  <img src="images/myItemsLdscp.png" height="200">
</a>

#### My Categories Screen
<a href="images/myCategories.png">
  <img src="images/myCategories.png" width="200">
</a>
<a href="images/myCategoriesLdscp.png">
  <img src="images/myCategoriesLdscp.png" height="200">
</a>

#### Transactions Screen
<a href="images/buying.png">
  <img src="images/buying.png" width="200">
</a>
<a href="images/selling.png">
  <img src="images/selling.png" width="200">
</a>
<a href="images/completed.png">
  <img src="images/completed.png" width="200">
</a>
<a href="images/completedLdscp.png">
  <img src="images/completedLdscp.png" height="200">
</a>

#### Add Item Dialog Fragment
<a href="images/addItem.png">
  <img src="images/addItem.png" width="200">
</a>
<a href="images/addItemLdscp.png">
  <img src="images/addItemLdscp.png" height="200">
</a>

#### Edit Item Dialog Fragment
<a href="images/editItem.png">
  <img src="images/editItem.png" width="200">
</a>
<a href="images/editItemLdscp.png">
  <img src="images/editItemLdscp.png" height="200">
</a>

#### View Item Details
<a href="images/requestItem.png">
  <img src="images/requestItem.png" width="200">
</a>
<a href="images/requestItemLdscp.png">
  <img src="images/requestItemLdscp.png" height="200">
</a>

#### Add Category Dialog Fragment
<a href="images/addCategory.png">
  <img src="images/addCategory.png" width="200">
</a>

#### View Available Categories
<a href="images/viewCategories.png">
  <img src="images/viewCategories.png" width="200">
</a>

## Prerequisites
- Android Studio installed (version 2025.1.4 or newer)
- JDK 17+
- Android SDK installed

## Setup
1. Clone this repository
```bash 
git clone https://github.com/nehaau2305/Trade-It.git
```
2. Open the project in Android Studio
3. Allow Android Studio to sync Gradle to ensure all dependencies are installed
4. Add a new device in the Device Manager to run the emulator or connect a physical Android device
5. Run the application using the "Run 'app'" button in the toolbar

## Contribution
- Angela Huang: Search functionality to brosw by category or query an item name, sort functionality to list items from latest post time or alphabetically, create new category functionality, edit category functiona.ity, delete category functionality, My Items fragment to view the user's items & categories, frontend design of the layouts.
- Nehaa Umapathy: Initialized the database tables with attributes, user authentication (account registration & login), item recycler view to list all items, add item functionality, edit item functionality, delete item functionality, transactions fragments to view pending (items to confirm the sale of & items awaiting the seller's confrimation) & completed transactions, toolbar menu, orientation change compatibility implementations.
