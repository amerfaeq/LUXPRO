#ifndef BOT_AUTOMATION_HPP
#define BOT_AUTOMATION_HPP

#include "core_engine.hpp"

namespace LuxCore {

    class BotAutomation {
    public:
        enum GameTable {
            LONDON,
            SYDNEY,
            MOSCOW,
            TOKYO,
            LAS_VEGAS,
            JAKARTA,
            TORONTO,
            CAIRO,
            DUBAI,
            SHANGHAI,
            PARIS,
            BANGKOK,
            SEOUL,
            MUMBAI,
            BERLIN
        };

        static void autoEnterLobby();
        static void selectFixedTable(GameTable table);
        static void handlePromotionPopups();
        
        static void setMode(int mode);
        static void setWinSequence(bool enabled);
        static void runGameFlowLoop();
    };

}

#endif // BOT_AUTOMATION_HPP
