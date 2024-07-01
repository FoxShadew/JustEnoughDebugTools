package dev.runefox.jedt.mixin;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.level.block.Rotation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

@Mixin(GameTestRegistry.class)
public abstract class GameTestRegistryMixin {
    // This redirect method does exactly what vanilla does, and yet we need it
    // because Fabric messed up GameTestRegistry in such a way that it now
    // throws an exception if you try to register tests through something else
    // than their API.
    //
    // Since Fabric injects into turnMethodIntoTestFunction, we redirect the
    // only ever usage of that to do exactly what the vanilla class does without
    // Fabric's injection. This means Fabric's testing API no longer works, but
    // we'll fix that.
    //
    // You did this to yourself, Fabric.

    @Redirect(
        method = "register(Ljava/lang/reflect/Method;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/gametest/framework/GameTestRegistry;" +
                     "turnMethodIntoTestFunction" +
                     "(Ljava/lang/reflect/Method;)" +
                     "Lnet/minecraft/gametest/framework/TestFunction;"
        )
    )
    private static TestFunction redirectTurnMethodIntoTestFunction(Method method) {
        GameTest gameTest = method.getAnnotation(GameTest.class);

        String cls = method.getDeclaringClass().getSimpleName();
        String suite = cls.toLowerCase();
        String test = suite + "." + method.getName().toLowerCase();
        String structure = gameTest.template().isEmpty() ? test : suite + "." + gameTest.template();
        String batch = gameTest.batch();

        Rotation rotation = StructureUtils.getRotationForRotationSteps(gameTest.rotationSteps());

        return new TestFunction(
            batch,
            test,
            structure,
            rotation,
            gameTest.timeoutTicks(),
            gameTest.setupTicks(),
            gameTest.required(),
            gameTest.manualOnly(),
            gameTest.attempts(),
            gameTest.requiredSuccesses(),
            gameTest.skyAccess(),
            jedtTurnMethodIntoConsumer(method)
        );
    }

    // Replaced this method to have actual generics to it
    private static <T> Consumer<T> jedtTurnMethodIntoConsumer(Method method) {
        return argument -> {
            try {
                Object instance = method.getDeclaringClass().newInstance();
                method.invoke(instance, argument);
            } catch (InvocationTargetException exc) {
                if (exc.getCause() instanceof RuntimeException target)
                    throw target;
                else
                    throw new RuntimeException(exc.getCause());
            } catch (ReflectiveOperationException exc) {
                throw new RuntimeException(exc);
            }
        };
    }
}
