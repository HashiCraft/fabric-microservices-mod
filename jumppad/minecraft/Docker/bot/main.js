const mineflayer = require('mineflayer')
const { pathfinder, Movements, goals: { GoalNear } } = require('mineflayer-pathfinder')

const botStartPos = process.env.SRE_BOT_START;
const botEndPos = process.env.SRE_BOT_END;

async function createBot() {
  await sleep(10000);

  if (!botStartPos || !botEndPos) {
    console.log("Please set SRE_BOT_START and SRE_BOT_END environment variables.");
    process.exit(1);
  }
 
  console.log("Starting bot at", botStartPos, "and moving to", botEndPos);

  const bot = mineflayer.createBot({
    host: 'localhost',
    port: 25565,
    username: 'SRE',
  })

  bot.loadPlugin(pathfinder)

  bot.once('spawn', () => {
    // teleport to the start point
    startPos = botEndPos.split(",");
    
    bot.chat('Hello, I am SRE bot, I am keeping your server safe. Telporting to start location.');
    bot.chat('/teleport ' + startPos[0] + ' ' + startPos[1] + ' ' + startPos[2]);

    moveBot(bot);
  });

  bot.on('error', (err) => console.log(err))
  bot.on('end', createBot);
}

async function moveBot(bot) {
  endPos = botStartPos.split(",");
  startPos = botEndPos.split(",");

  while(true) {
    await sleep(20000);
    navigateTo(bot, startPos[0], startPos[1], startPos[2]);

    await sleep(20000);
    navigateTo(bot, endPos[0], endPos[1], endPos[2]);
  }
}

function navigateTo(bot, x, y, z) {
  const defaultMove = new Movements(bot)

  bot.pathfinder.setGoal(new GoalNear(x, y, z, 2))
}

function sleep(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

// start the bot
createBot();