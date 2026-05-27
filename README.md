# সমাজ কোরবানী ব্যবস্থাপনা

এই প্রোজেক্টটি একটি static HTML app, যা GitHub Pages-এ publish করা যাবে এবং চাইলে Supabase cloud sync-ও ব্যবহার করতে পারবে।

## Files

- `index.html` : GitHub Pages-এর জন্য ready main app
- `ai_studio_code (12).html` : original working copy
- `supabase-setup.sql` : Supabase database table/policy setup

## GitHub Pages Publish

1. একটি নতুন GitHub repository তৈরি করুন
2. `index.html`, `README.md`, এবং `supabase-setup.sql` upload করুন
3. GitHub repo-তে `Settings > Pages` এ যান
4. `Deploy from a branch` নির্বাচন করুন
5. Branch হিসেবে `main` এবং folder হিসেবে `/ (root)` দিন
6. Save করার পর কয়েক মিনিটের মধ্যে live link পাবেন

## Supabase Setup

1. Supabase project তৈরি করুন
2. SQL Editor-এ `supabase-setup.sql` run করুন
3. App-এর Settings খুলুন
4. `Supabase Cloud Sync` চালু করুন
5. `Project URL`, `Anon Key`, এবং `Cloud Record ID` দিন
6. `Cloud Save` চাপুন

## Notes

- Internet connection ছাড়া CDN-based style/script পুরোপুরি load নাও হতে পারে
- `localStorage` fallback আছে, তাই cloud off থাকলেও app চলবে
- Production use-এর আগে Supabase RLS policy আরও restrict করা ভালো
