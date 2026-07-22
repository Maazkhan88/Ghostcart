-- Seeds the initial category picklist from the distinct category values
-- already used across the current products (avoids the dropdown starting
-- empty and forcing an admin to retype every category by hand).
INSERT INTO categories (name) VALUES
  ('Apparel'), ('Beauty'), ('Coffee & Drinks'), ('Delivery'), ('Electronics'),
  ('Fashion'), ('Fast Food'), ('Food & Coffee'), ('Gadgets & Tech'), ('Gaming'),
  ('Healthy'), ('Home'), ('Jewellery'), ('Music');
