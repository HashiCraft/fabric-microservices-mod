\c minecraft;
CREATE TABLE counter (
  count INT NOT NULL
);

CREATE TABLE Blocks (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- Natural Blocks
INSERT INTO Blocks (name) VALUES ('Dirt'), ('Grass Block'), ('Stone'), ('Sand'), ('Gravel'), ('Cobblestone'), ('Bedrock'), ('Ice'), ('Packed Ice'), ('Snow Block');

-- Ores
INSERT INTO Blocks (name) VALUES ('Coal Ore'), ('Iron Ore'), ('Gold Ore'), ('Redstone Ore'), ('Diamond Ore'), ('Emerald Ore'), ('Lapis Lazuli Ore');

-- Building Blocks
INSERT INTO Blocks (name) VALUES ('Wood Planks'), ('Bricks'), ('Stone Bricks'), ('Sandstone'), ('Nether Bricks'), ('Quartz Block'), ('Prismarine'), ('Purpur Block'), ('Concrete'), ('Terracotta');

-- Decorative Blocks
INSERT INTO Blocks (name) VALUES ('Bookshelf'), ('Flower Pot'), ('Painting'), ('Anvil'), ('Enchanting Table'), ('Jukebox'), ('Note Block'), ('Beacon'), ('End Rod'), ('Sea Lantern');

-- Redstone Components
INSERT INTO Blocks (name) VALUES ('Redstone Dust'), ('Redstone Torch'), ('Redstone Repeater'), ('Redstone Comparator'), ('Piston'), ('Sticky Piston'), ('Observer'), ('Dispenser'), ('Dropper'), ('Hopper');

-- Plants and Crops
INSERT INTO Blocks (name) VALUES ('Wheat'), ('Carrots'), ('Potatoes'), ('Pumpkin'), ('Melon'), ('Sugarcane'), ('Cactus'), ('Bamboo'), ('Nether Wart'), ('Chorus Plant');

-- Liquids
INSERT INTO Blocks (name) VALUES ('Water Source Block'), ('Lava Source Block'), ('Waterlogged Blocks');

-- Transportation
INSERT INTO Blocks (name) VALUES ('Rail'), ('Powered Rail'), ('Detector Rail'), ('Activator Rail');

-- Monster Spawners
INSERT INTO Blocks (name) VALUES ('Zombie Spawner'), ('Skeleton Spawner'), ('Spider Spawner'), ('Creeper Spawner');

-- Miscellaneous
INSERT INTO Blocks (name) VALUES ('Chest'), ('Furnace'), ('Crafting Table'), ('Bed'), ('Ender Chest'), ('Trapped Chest'), ('Anvil'), ('End Portal Frame'), ('Dragon Egg'), ('Beacon Block');