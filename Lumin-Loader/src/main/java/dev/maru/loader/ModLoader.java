package dev.maru.loader;

import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import niurendeobf.ZKMIndy;

@ZKMIndy
public class ModLoader implements IModFileCandidateLocator {

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        
    }

}
