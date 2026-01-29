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


#### Account Registration Screen


#### Login Screen


#### Authenticated Home Screen


#### My Items Screen


#### My Categories Screen


#### Transactions Screen


#### Add Item Dialog Fragment


#### Edit Item Dialog Fragment


#### View Item Details


#### Add Category Dialog Fragment



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
