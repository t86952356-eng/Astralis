package astralis.account;

import astralis.social.model.MicrosoftClient;

public interface MicrosoftClients {
    MicrosoftClient IAS_CLIENT = MicrosoftClientsInitialization.iasClient;
    MicrosoftClient LAUNCHER_CLIENT = MicrosoftClientsInitialization.launcherClient;
}
