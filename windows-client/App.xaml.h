#pragma once

#include "pch.h"
#include "App.xaml.g.h"

namespace winrt::ZizuWindowsClient::implementation
{
    struct App : AppT<App>
    {
        App();
        ~App();

        void OnLaunched(Microsoft::UI::Xaml::LaunchActivatedEventArgs const& args);

    private:
        bool acquireSingleInstance();
        bool hasHiddenLaunchArgument() const;

        Microsoft::UI::Xaml::Window m_window{ nullptr };
        HANDLE m_mutexHandle{ nullptr };
    };
}
