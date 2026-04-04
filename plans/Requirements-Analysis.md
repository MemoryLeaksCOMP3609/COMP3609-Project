Game Overview

This game is inspired by the mechanics of Vampire Survivors, featuring fast-paced survival

gameplay where the player fights continuous waves of enemies, collects experience, and

upgrades their abilities over time.

The objective of the game is to survive, grow stronger, progress through regions, and

ultimately defeat the final boss.

Game Entities

Player

?  Has attributes such as:

?  Health (HP)

?  Movement Speed

Damage

?  Fire Rate

?  Collects crystals dropped from killed mobs to upgrades abilities

Enemies

Different enemy types will spawn and attack the player:

?  Bat Basic low-health enemy

?  Skeleton Normal difficulty enemy

?  Ghost Potentially faster or harder-to-hit enemy

?  Killing an enemy grants the Player Points which can be used to upgrade skills when

they level up.

?  Upon death, enemies have a chance to drop:

?  Nothing

?  A Health Orb, which can be picked up to regain some lost health

?  An Experience Crystal

Boss

The boss is an Abomination that appears when the player reaches a level milestone - 5, 10,

15.

When the boss reaches 5% health, it retreats to the next region, where it will feature more

health and abilities.

?  Level 5

?  Uses melee attacks

?  Level 10

?  Uses ranged attacks

?  Level 15

?  Uses both melee and ranged attacks

?  Splits into 3 smaller entities each with 25% power

?  Each of these entities split again into weaker versions each having 5% total

power

Regions (Maps)

The game will include multiple playable environments:

?  Grass Region (levels 1 - 5)

?  Desert Region (levels 6 - 10)

?  Volcano Region (levels 11 - 15)

?  Wasteland (tentative)

?  Glacial Map (tentative)

Each region represents a game level and  introduces different boss mechanics and mob

difficulty scaling.

Gameplay Mechanics

Core Gameplay Loop

The main gameplay cycle consists of:

1.  Player defeats enemies and gains points

2.  Enemies drop:

?  Experience Crystals

?  Health Orbs

3.  Player collects drops

4.  Player gains experience and levels up

5.  Modal shop appears and player uses points to buy upgrades

6.  Stronger enemies and bosses appear

Scoring System

?  Players earn points by killing enemies

?  Players spend points to buy upgrades

?  The highscores will be based on final points, so the ability to beat the game while

spending less points on upgrades will net you a high total score

Player Progression

?  Player levels increase as XP is collected

?  Bosses appear every 5 player levels

?  The player progresses through maps

Win Condition

?  Reach Map 3

?  Defeat the boss in his final form

Loss Condition

?  Player Health (HP) reaches 0

Upgrade System (Shop)

When leveling up, a popup shop appears offering upgrades:

?

?

?

?

Increase Damage

Increase Movement Speed

Increase Fire Rate

Increase Maximum Health

Each upgrade costs points earned from killing enemies.

Implementation Approach

Game Loop

The game will use a standard game loop consisting of:

?

Input handling

?  Game state updating (movement, collisions, enemy AI, pickups)

?  Rendering graphics to the screen

Double buffering will be used to prevent screen flickering and ensure smooth rendering.

Object-Oriented Design

The system will be designed using OOP principles. The main classes will include:

?  Player

?  Enemy (parent class)

?  Bat

?  Skeleton

?  Ghost

?  Boss

?  Projectile

?  Solid Terrain Objects

?

Item

?  Experience Crystal

?  Health Orb

?  Maps / Regions

?  GamePanel

?  Sound

?  Scoreboard / UI

Inheritance will be used for enemy types, and encapsulation will be used to manage player

statistics such as health, damage, speed, and fire rate.

Graphics and Animation

Graphics will be implemented using Java Graphics2D. The following techniques will be used:

?  Sprite sheets for character animations

?  Strip animation for player and enemy movement

?

Image effects (ImageFX) such as:

?  Grayscale effect on game over

?  Red tint effect when player health is low

?  Transparency effect for ghost enemies

?  Scrolling game panel when moving along the world map with player in the centre.

Tile Based Map System

The game world will be implemented using a tile based map system. Each region (Grass,

Desert, Volcano) will be constructed using a 2D grid of tiles, and each tile will represent a

terrain type such as grass, sand, lava.

The tile map will be stored as a 2D array, where each number corresponds to a reference of

the tile image. A tile manager class will be responsible for:

?  Loading tile images

?  Rendering visible tiles to the screen

The tile-based system improves performance by rendering only the visible portion of the map

and allows maps to be designed easily using tile layouts.

Collision Detection

Collision detection will be implemented using rectangular bounding boxes. Collision handling

will include:

?  Player and enemy collisions

?  Projectile and enemy collisions

?  Player and item pickup collisions

?  Player and solid terrain object collisions (trees, rocks, ponds)

Enemy Movement and AI

Enemy movement will be implemented using pathing behaviors such as:

?  Direct movement toward the player

?  Bezier curve movement for certain enemy types to create more complex motion

patterns

?  Boss entities will have multiple attack patterns depending on their phase

User Interface

The game will include:

?  Main menu

?  Pause menu

?  Upgrade shop popup

?  Health bar

?  Experience bar

?  Score display

?  Game over screen

Audio

Sound effects will be implemented for:

?  Player attacks

?  Enemy hits and deaths

?

Item pickups

?  Boss attacks

?  Menu interactions

Background music will be played during gameplay and boss fights.

Game States

The game will be managed using different game states, such as:

?  Menu State

?  Playing State

?  Pause State

?  Shop State

?  Game Over State

State management will control what is updated and rendered at any given time.

Summary

This game is a survival based arcade action game where the player continuously fights

enemies, collects resources, upgrades abilities, and progresses through increasingly difficult

stages to defeat a final boss.