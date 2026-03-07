package dev.maru.loader;

import by.radioegor146.nativeobfuscator.Native;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import niurendeobf.ZKMIndy;

@ZKMIndy
@Native
public class ModLoader implements IModFileCandidateLocator {

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {

        Loader.load(pipeline, this);

    }

}
