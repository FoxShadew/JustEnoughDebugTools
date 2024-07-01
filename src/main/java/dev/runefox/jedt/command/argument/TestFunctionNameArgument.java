package dev.runefox.jedt.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class TestFunctionNameArgument implements ArgumentType<String> {
   private static final Collection<String> EXAMPLES = Arrays.asList("techtests.piston", "techtests");

   @Override
   public String parse(StringReader reader) {
      return reader.readUnquotedString();
   }

   public static TestFunctionNameArgument testFunctionNameArgument() {
      return new TestFunctionNameArgument();
   }

   public static String getTestFunction(CommandContext<CommandSourceStack> ctx, String name) {
      return ctx.getArgument(name, String.class);
   }

   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> ctx, SuggestionsBuilder builder) {
       Stream<String> fns = GameTestRegistry.getAllTestFunctions().stream().map(TestFunction::testName);
      return SharedSuggestionProvider.suggest(fns, builder);
   }

   @Override
   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
