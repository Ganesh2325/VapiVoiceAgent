package com.voiceos.provider.calendar;

import com.voiceos.provider.ProviderRequest;
import com.voiceos.provider.ProviderResult;
import com.voiceos.provider.ServiceProvider;

public interface CalendarProvider extends ServiceProvider {

    ProviderResult read(ProviderRequest request);

    ProviderResult write(ProviderRequest request);
}
