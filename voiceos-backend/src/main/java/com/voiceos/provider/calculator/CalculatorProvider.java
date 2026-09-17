package com.voiceos.provider.calculator;

import com.voiceos.provider.ProviderMode;
import com.voiceos.provider.ProviderResult;
import com.voiceos.provider.ServiceProvider;

/**
 * Local arithmetic provider used by {@code CalculatorTool}.
 */
public interface CalculatorProvider extends ServiceProvider {

    ProviderMode mode();

    ProviderResult evaluate(String expression);
}
